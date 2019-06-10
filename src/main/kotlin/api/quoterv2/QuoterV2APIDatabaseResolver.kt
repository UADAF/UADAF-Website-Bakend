package api.quoterv2

import dao.QuoterV2
import model.QuoteV2
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object QuoterV2APIDatabaseResolver : IQuoterV2APIResolver {

    override fun addQuote(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>): Unit = transaction {
        QuoterV2.insert {
            it[adder] = adderIn
            it[authors] = authorsIn
            it[content] = contentIn
            it[dtype] = displayTypeIn
            it[attachments] = attachmentsIn.joinToString(";")
        }
    }

    override fun getById(id: Int): List<QuoteV2> = transaction {
        QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2)
    }

    override fun getRange(from: Int, to: Int): List<QuoteV2> = transaction {
        QuoterV2.select { (QuoterV2.id greaterEq from) and (QuoterV2.id lessEq to) }.map(::QuoteV2)
    }

    override fun getTotal(): Int = transaction { QuoterV2.selectAll().count() }

    override fun getRandom(c: Int): List<QuoteV2> = transaction {
        val total =  QuoterV2.selectAll().count()
        val count = Integer.min(c, total)
        val indexes = (0..total).toMutableList()

        for (i in 0..(total - count)) {
            indexes.removeAt(Random.nextInt(indexes.size))
        }

        return@transaction QuoterV2.select { QuoterV2.id inList indexes }.map(::QuoteV2)
    }

    override fun fixIds() = transaction {
        val allIds = QuoterV2.slice(QuoterV2.id).selectAll().map { it[QuoterV2.id] }

        allIds.forEachIndexed { index, id ->
            if (index + 1 != id) {
                QuoterV2.update({
                    QuoterV2.id eq id
                }) {
                    it[QuoterV2.id] = index + 1
                }
            }
        }
    }

    override fun getAll(): List<QuoteV2> = transaction {
        QuoterV2.selectAll().map(::QuoteV2)
    }

    override fun isExists(id: Int): Boolean = transaction {
        QuoterV2.select { QuoterV2.id eq id }.count() > 0
    }

    override fun addAttachment(id: Int, attachment: String): QuoterV2Api.AttachmentResult = transaction {
        val oldAttachments = QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2).first().attachments.toTypedArray()
        if (attachment in oldAttachments) return@transaction QuoterV2Api.AttachmentResult.AlreadyAttached
        val newAttachments = listOf(*oldAttachments, attachment).joinToString(";")

        return@transaction if (QuoterV2.update({ QuoterV2.id eq id }) { it[attachments] = newAttachments } != 0) {
            QuoterV2Api.AttachmentResult.Attached
        } else {
            QuoterV2Api.AttachmentResult.Error
        }
    }

    override fun editQuote(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String) = transaction {
        val oldContent = QuoterV2.select { QuoterV2.id eq idIn }.map(::QuoteV2).first().content
        QuoterV2.update({ QuoterV2.id eq idIn }) {
            it[editedBy] = editedByIn
            it[editedAt] = editedAtIn
            it[previousContent] = oldContent
            it[content] = newContentIn
        } != 0
    }


}