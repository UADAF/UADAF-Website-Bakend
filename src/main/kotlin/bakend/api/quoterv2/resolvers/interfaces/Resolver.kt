package bakend.api.quoterv2.resolvers.interfaces

import bakend.model.QuoteV2
import kotlin.math.min


interface Resolver {

    fun total(): Int

    fun byId(id: Int): QuoteV2 {
        val res = byList(listOf(id))
        require(res.isNotEmpty())
        return res[0]
    }

    /**
     * If quote does not exist, it should be skipped
     */
    fun byList(ids: List<Int>) = ids.mapNotNull {
        try {
            byId(it)
        } catch (e: Exception) {
            null
        }
    }

    fun exists(id: Int) = byList(listOf(id)).isNotEmpty()

    fun range(from: Int, to: Int) = byList((from..to).toList())

    fun all(): List<QuoteV2> = byList((1..total()).toList())

    fun random(c: Int): List<QuoteV2> {
        val total = total()
        val count = min(c, total)
        val seq = randomSequence(total)
        val ret = mutableListOf<QuoteV2>()
        try {
            while (ret.size < count) {
                ret.addAll(byList(seq.take(count - ret.size).toList()))
            }
        } catch(e: NoSuchElementException) {} //All quotes ended before we could gather enough
        return ret
    }

    private fun randomSequence(total: Int) = (1..total).toList().shuffled().asSequence()

}