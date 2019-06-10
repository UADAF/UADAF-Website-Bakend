package bakend.api.quoterv2.resolvers

import bakend.api.quoterv2.QuoterV2Api

abstract class ReadOnlyResolver : IQuoterV2APIResolver {

    override fun add(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>) {
        throw NotImplementedError("Can't add to ReadOnlyResolvev")
    }

    override fun attach(id: Int, attachment: String): QuoterV2Api.AttachmentResult {
        throw NotImplementedError("Can't add attachment to ReadOnlyResolver")
    }

    override fun edit(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean {
        throw NotImplementedError("Can't edit quote in ReadOnlyResolver")
    }

    override fun fixIds() {
        throw NotImplementedError("Can't fix ids in ReadOnlyResolver")
    }

}