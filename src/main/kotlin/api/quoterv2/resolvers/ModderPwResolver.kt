package api.quoterv2.resolvers

import com.google.gson.JsonObject
import com.gt22.uadam.utils.*
import dao.QuoterTable
import model.QuoteV2
import java.lang.Integer.min
import java.lang.StringBuilder
import java.net.URL
import kotlin.random.Random

object ModderPwResolver : ReadOnlyResolver() {

    private val attachmentPattern = """^!\[(.+)]\(.+\)$""".toRegex()

    //TODO: Replace with normal http framework
    private fun parse(url: String) = PARSER.parse(URL(url).openStream().bufferedReader()).obj

    private operator fun Regex.get(s: String, id: Int = 1) = find(s)!!.groupValues[id]

    private fun unwrapJson(q: JsonObject): QuoteV2 {
        val attachments = mutableListOf<String>()
        val text = StringBuilder()
        for (line in q["text"].str.split("\r\n")) {
            if (line.matches(attachmentPattern)) {
                attachments.add(attachmentPattern[line])
            } else {
                text.append(line).append('\n')
            }
        }
        return QuoteV2(q["id"].int, q["creator"].str, listOf("Цитатник Моддера"),
                if(text.count { it == '\n' } > 1) "dialog" else "text",
                text.toString(), q["created_at"].asLong, "null", -1, attachments, false)
    }

    private fun getQuote(id: Int) = unwrapJson(parse("https://modder.pw/api/get.php?id=$id"))

    override fun getById(table: QuoterTable, id: Int): List<QuoteV2> {
        return listOf(getQuote(id))
    }

    override fun getRange(table: QuoterTable, from: Int, to: Int): List<QuoteV2> {
        val ret = mutableListOf<QuoteV2>()
        for(i in from..to) {
            ret.add(getQuote(i))
        }
        return ret
    }

    fun String.extractBetween(start: String, end: String): String {
        val nStart = indexOf(start) + start.length
        val nEnd = indexOf(end, nStart)
        return substring(nStart, nEnd)
    }

    override fun getTotal(table: QuoterTable): Int {
        val html = URL("https://modder.pw/new/").readText()
        return html.extractBetween("Цитата #", "</a>").toInt()
    }

    override fun getRandom(table: QuoterTable, c: Int): List<QuoteV2> {
        val ids = mutableSetOf<Int>()
        val total = getTotal(table)
        if(c >= total) {
            return getAll(table)
        }
        while(ids.size < c) {
            ids.add(Random.nextInt(1, total + 1))
        }
        return ids.map(::getQuote)
    }

    override fun getAll(table: QuoterTable): List<QuoteV2> {
        val total = getTotal(table)
        val ret = mutableListOf<QuoteV2>()
        for (i in 1..total) {
            ret.add(getQuote(i))
        }
        return ret
    }

    override fun isExists(table: QuoterTable, id: Int): Boolean {
        return parse("https://modder.pw/api/get.php?id=$id")["success"].bln
    }
}