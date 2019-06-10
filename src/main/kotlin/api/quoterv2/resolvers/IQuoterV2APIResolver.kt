package api.quoterv2.resolvers

import api.quoterv2.QuoterV2Api
import model.QuoteV2


interface IQuoterV2APIResolver {

    fun add(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>)

    fun byId(id: Int): QuoteV2

    fun range(from: Int, to: Int): List<QuoteV2>

    fun total(): Int

    fun random(c: Int): List<QuoteV2>

    fun all(): List<QuoteV2>

    fun exists(id: Int): Boolean

    fun attach(id: Int, attachment: String): QuoterV2Api.AttachmentResult

    fun edit(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String): Boolean

    fun fixIds()

}