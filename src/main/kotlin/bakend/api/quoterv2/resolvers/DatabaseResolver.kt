package bakend.api.quoterv2.resolvers

import bakend.api.quoterv2.QuoterV2Api
import bakend.api.quoterv2.resolvers.interfaces.WritableResolver
import bakend.dao.QuoterTable
import bakend.model.QuoteV2
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class QuoterV2APIDatabaseResolver(private val table: QuoterTable) : WritableResolver {

    override fun total(): Int = transaction { table.selectAll().count() }

    override fun byList(ids: List<Int>) = transaction {
        table.select { table.id inList ids }.map { table to it }.map(::QuoteV2)
    }



    override fun add(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>): Unit = transaction {
        table.insert {
            it[adder] = adderIn
            it[authors] = authorsIn
            it[content] = contentIn
            it[dtype] = displayTypeIn
            it[attachments] = attachmentsIn.joinToString(";")
        }
    }

    override fun attach(id: Int, attachment: String): QuoterV2Api.AttachmentResult = transaction {
        val oldAttachments = table.select { table.id eq id }.map { table to it }.map(::QuoteV2).first().attachments.toTypedArray()
        if (attachment in oldAttachments) return@transaction QuoterV2Api.AttachmentResult.AlreadyAttached
        val newAttachments = listOf(*oldAttachments, attachment).joinToString(";")

        return@transaction if (table.update({ table.id eq id }) { it[attachments] = newAttachments } != 0) {
            QuoterV2Api.AttachmentResult.Attached
        } else {
            QuoterV2Api.AttachmentResult.Error
        }
    }

    override fun edit(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String) = transaction {
        val oldContent = table.select { table.id eq idIn }.map { table to it }.map(::QuoteV2).first().content
        table.update({ table.id eq idIn }) {
            it[editedBy] = editedByIn
            it[editedAt] = editedAtIn
            it[previousContent] = oldContent
            it[content] = newContentIn
        } != 0
    }

    override fun fixIds(): Unit = transaction {
        val allIds = table.slice(table.id).selectAll().map { it[table.id] }

        allIds.forEachIndexed { index, id ->
            if (index + 1 != id) {
                table.update({
                    table.id eq id
                }) {
                    it[table.id] = index + 1
                }
            }
        }

        exec("ALTER TABLE `${table.tableName}` AUTO_INCREMENT = ${allIds.size}")
    }
}