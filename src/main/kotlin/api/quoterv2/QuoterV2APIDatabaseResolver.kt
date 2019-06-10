package api.quoterv2

import dao.QuoterTable
import model.QuoteV2
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object QuoterV2APIDatabaseResolver : IQuoterV2APIResolver {

    override fun addQuote(table: QuoterTable, adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>): Unit = transaction {
        table.insert {
            it[adder] = adderIn
            it[authors] = authorsIn
            it[content] = contentIn
            it[dtype] = displayTypeIn
            it[attachments] = attachmentsIn.joinToString(";")
        }
    }

    override fun getById(table: QuoterTable, id: Int): List<QuoteV2> = transaction {
        table.select { table.id eq id }.map(::QuoteV2)
    }

    override fun getRange(table: QuoterTable, from: Int, to: Int): List<QuoteV2> = transaction {
        table.select { (table.id greaterEq from) and (table.id lessEq to) }.map(::QuoteV2)
    }

    override fun getTotal(table: QuoterTable): Int = transaction { table.selectAll().count() }

    override fun getRandom(table: QuoterTable, c: Int): List<QuoteV2> = transaction {
        val total =  table.selectAll().count()
        val count = Integer.min(c, total)
        val indexes = (0..total).toMutableList()

        for (i in 0..(total - count)) {
            indexes.removeAt(Random.nextInt(indexes.size))
        }

        return@transaction table.select { table.id inList indexes }.map(::QuoteV2)
    }

    override fun fixIds(table: QuoterTable) = transaction {
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
    }

    override fun getAll(table: QuoterTable): List<QuoteV2> = transaction {
        table.selectAll().map(::QuoteV2)
    }

    override fun isExists(table: QuoterTable, id: Int): Boolean = transaction {
        table.select { table.id eq id }.count() > 0
    }

    override fun addAttachment(table: QuoterTable, id: Int, attachment: String): QuoterV2Api.AttachmentResult = transaction {
        val oldAttachments = table.select { table.id eq id }.map(::QuoteV2).first().attachments.toTypedArray()
        if (attachment in oldAttachments) return@transaction QuoterV2Api.AttachmentResult.AlreadyAttached
        val newAttachments = listOf(*oldAttachments, attachment).joinToString(";")

        return@transaction if (table.update({ table.id eq id }) { it[attachments] = newAttachments } != 0) {
            QuoterV2Api.AttachmentResult.Attached
        } else {
            QuoterV2Api.AttachmentResult.Error
        }
    }

    override fun editQuote(table: QuoterTable, idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String) = transaction {
        val oldContent = table.select { table.id eq idIn }.map(::QuoteV2).first().content
        table.update({ table.id eq idIn }) {
            it[editedBy] = editedByIn
            it[editedAt] = editedAtIn
            it[previousContent] = oldContent
            it[content] = newContentIn
        } != 0
    }


}