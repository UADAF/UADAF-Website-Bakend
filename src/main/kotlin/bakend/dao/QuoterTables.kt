package bakend.dao

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

open class QuoterTable(name: String) : Table(name) {
    val id = integer("id").primaryKey().autoIncrement()
    val adder = text("adder")
    val authors = text("authors")
    val date = datetime("date").defaultExpression(CurrentDateTime())
    val dtype = text("dtype")
    val content = text("content")
    val editedBy = text("edited_by").nullable()
    val editedAt = long("edited_at").nullable()
    val previousContent = text("previous_content").nullable()
    val attachments = text("attachments")
    val isOld = bool("is_old").default(false)
}

private val registeredTables: MutableMap<String, QuoterTable> = mutableMapOf()

fun getTable(name: String, createIfNotExist: Boolean = false): QuoterTable? {
    if (name in registeredTables) {
        return registeredTables[name]
    }
    val table = QuoterTable("quoter_$name")
    return when {
        table.exists() -> {
            registeredTables[name] = table
            table
        }
        createIfNotExist -> {
            SchemaUtils.create(table)
            table
        }
        else -> null
    }
}

val QuoterV2 = transaction { getTable("v2", true)!! }