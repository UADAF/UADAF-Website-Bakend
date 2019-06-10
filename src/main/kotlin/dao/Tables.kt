package dao

import org.jetbrains.exposed.sql.Table

object Quoter : Table("quoter") {
    val id = integer("id").primaryKey().autoIncrement()
    val adder = text("adder")
    val author = text("author")
    val quote = text("quote")
    val editedBy = text("edited_by").nullable()
    val editedAt = long("edited_at").nullable()
}

object Users : Table("users") {
    val user = varchar("user", 255).primaryKey()
    val story = integer("story")
    val rate = integer("rate")
}

object Attachments : Table("attachments") {
    val id = text("id")
    val type = text("type")
    val data = blob("data")
}
