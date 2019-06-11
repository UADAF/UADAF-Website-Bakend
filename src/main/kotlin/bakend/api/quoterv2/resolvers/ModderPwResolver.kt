package bakend.api.quoterv2.resolvers

import com.google.gson.JsonObject
import com.gt22.uadam.utils.*
import bakend.model.QuoteV2
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
        if(q["text"] == null) {
            throw IllegalArgumentException("This quote doesn't exist")
        }
        for (line in q["text"].str.split("\r\n")) {
            if (line.matches(attachmentPattern)) {
                attachments.add("https://modder.pw/${attachmentPattern[line]}")
            } else {
                text.append(line).append('\n')
            }
        }
        return QuoteV2(q["id"].int, q["creator"].str, listOf("Цитатник Моддера"),
                if (text.count { it == '\n' } > 1) "dialog" else "text",
                text.toString(), q["created_at"].asLong, "null", -1, attachments, false)
    }

    override fun byId(id: Int): QuoteV2 = unwrapJson(parse("https://modder.pw/api/get.php?id=$id"))

    override fun range(from: Int, to: Int): List<QuoteV2> {
        val ret = mutableListOf<QuoteV2>()
        for(i in from..to) {
            ret.add(byId(i))
        }
        return ret
    }

    fun String.extractBetween(start: String, end: String): String {
        val nStart = indexOf(start) + start.length
        val nEnd = indexOf(end, nStart)
        return substring(nStart, nEnd)
    }

    override fun total(): Int {
        val html = URL("https://modder.pw/new/").readText()
        return html.extractBetween("Цитата #", "</a>").toInt()
    }

    override fun random(c: Int): List<QuoteV2> {
        val quotes = mutableListOf<QuoteV2>()
        var total = total()

        while(quotes.size < c) {
            if(c >= total) {
                return all()
            }
            val id = Random.nextInt(1, total + 1)
            try {
                quotes.add(byId(id))
            } catch (e: java.lang.IllegalArgumentException) {
                total--
            }
        }
        return quotes
    }

    override fun all(): List<QuoteV2> {
        val total = total()
        val ret = mutableListOf<QuoteV2>()
        for (i in 1..total) {
            ret.add(byId(i))
        }
        return ret
    }

    override fun exists(id: Int): Boolean {
        return parse("https://modder.pw/api/get.php?id=$id")["success"].bln
    }
}