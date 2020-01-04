package bakend.api.quoterv2.resolvers.interfaces

import bakend.model.QuoteV2

interface SearchableResolver : Resolver {

    fun search(adder: String?, authors: List<String>?, content: String?): List<QuoteV2>

}