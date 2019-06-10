package api.quoterv2.resolvers

import api.quoterv2.IQuoterV2APIResolver
import api.quoterv2.QuoterV2Api

abstract class ReadOnlyResolver : IQuoterV2APIResolver {

    override fun addQuote(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>) {
        throw NotImplementedError("Can't add to ReadOnlyResolvev")
    }

    override fun addAttachment(id: Int, attachment: String): QuoterV2Api.AttachmentResult {
        throw NotImplementedError("Can't add attachment to ReadOnlyResolver")
    }

    override fun editQuote(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean {
        throw NotImplementedError("Can't edit quote in ReadOnlyResolver")
    }

    override fun fixIds() {
        throw NotImplementedError("Can't fix ids in ReadOnlyResolver")
    }

}