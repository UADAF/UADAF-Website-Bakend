package api.quoterv2

import model.QuoteV2


interface IQuoterV2APIResolver {

    fun addQuote(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>)

    fun getById(id: Int): List<QuoteV2>

    fun getRange(from: Int, to: Int): List<QuoteV2>

    fun getTotal(): Int

    fun getRandom(c: Int): List<QuoteV2>

    fun getAll(): List<QuoteV2>

    fun isExists(id: Int): Boolean

    fun addAttachment(id: Int, attachment: String): QuoterV2Api.AttachmentResult

    fun editQuote(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean

    fun fixIds()

}