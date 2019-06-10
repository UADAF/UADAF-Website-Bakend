package api.quoterv2

import dao.QuoterTable
import model.QuoteV2


interface IQuoterV2APIResolver {

    fun addQuote(table: QuoterTable, adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>)

    fun getById(table: QuoterTable, id: Int): List<QuoteV2>

    fun getRange(table: QuoterTable, from: Int, to: Int): List<QuoteV2>

    fun getTotal(table: QuoterTable): Int

    fun getRandom(table: QuoterTable, c: Int): List<QuoteV2>

    fun getAll(table: QuoterTable): List<QuoteV2>

    fun isExists(table: QuoterTable, id: Int): Boolean

    fun addAttachment(table: QuoterTable, id: Int, attachment: String): QuoterV2Api.AttachmentResult

    fun editQuote(table: QuoterTable, idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean

    fun fixIds(table: QuoterTable)

}