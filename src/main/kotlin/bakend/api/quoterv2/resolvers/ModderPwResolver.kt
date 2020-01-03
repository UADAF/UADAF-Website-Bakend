package bakend.api.quoterv2.resolvers

import bakend.api.quoterv2.resolvers.interfaces.Resolver
import bakend.model.QuoteV2
import com.google.gson.JsonObject
import com.gt22.uadam.utils.PARSER
import com.gt22.uadam.utils.int
import com.gt22.uadam.utils.obj
import com.gt22.uadam.utils.str
import java.net.URL

object ModderPwResolver : Resolver {

    private val attachmentPattern = """^!\[(.+)]\(.+\)$""".toRegex()
    //TODO: Replace with normal http framework
    private fun parse(url: String) = PARSER.parse(URL(url).openStream().bufferedReader()).obj

    private operator fun Regex.get(s: String, id: Int = 1) = find(s)!!.groupValues[id]

    private fun unwrapJson(q: JsonObject): QuoteV2 {
        val attachments = mutableListOf<String>()
        val text = StringBuilder()
        requireNotNull(q["text"]) { "This quote doesn't exist" }
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

    private fun String.extractBetween(start: String, end: String): String {
        val nStart = indexOf(start) + start.length
        val nEnd = indexOf(end, nStart)
        return substring(nStart, nEnd)
    }

    override fun total(): Int {
        val html = URL("https://modder.pw/new/").readText()
        return html.extractBetween("Цитата #", "</a>").toInt()
    }
}