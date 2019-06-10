package api.quoterv2

import model.QuoteV2

object QuoterV2APITestResolver : IQuoterV2APIResolver {

    override fun addQuote(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getById(id: Int): List<QuoteV2> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRange(from: Int, to: Int): List<QuoteV2> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTotal(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRandom(c: Int): List<QuoteV2> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(): List<QuoteV2> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExists(id: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addAttachment(id: Int, attachment: String): QuoterV2Api.AttachmentResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun editQuote(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fixIds() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}