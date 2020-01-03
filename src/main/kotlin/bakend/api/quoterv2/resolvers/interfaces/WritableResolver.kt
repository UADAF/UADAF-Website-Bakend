package bakend.api.quoterv2.resolvers.interfaces

import bakend.api.quoterv2.QuoterV2Api

interface WritableResolver : Resolver {

    fun add(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>)

    fun attach(id: Int, attachment: String): QuoterV2Api.AttachmentResult

    fun edit(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean

    fun fixIds()

}