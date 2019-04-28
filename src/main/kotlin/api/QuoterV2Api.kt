package api

import dao.QuoterV2
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import model.QuoteV2
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException
import kotlin.random.Random

object QuoterV2Api {

    fun addQuote(adder: String, author: String, content: String, attachments: List<String>): Unit = transaction {
        QuoterV2.insert {
            it[QuoterV2.adder] = adder
            it[QuoterV2.author] = author
            it[QuoterV2.content] = content
            it[QuoterV2.attachments] = attachments.joinToString(";")
        }
    }

    fun getById(id: Int): List<QuoteV2> = transaction {
        QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2)
    }

    fun getRange(from: Int, to: Int): List<QuoteV2> = transaction {
        QuoterV2.select { (QuoterV2.id greaterEq from) and (QuoterV2.id lessEq to) }.map(::QuoteV2)
    }

    fun getTotal(): Int = QuoterV2.selectAll().count()

    fun getRandom(count: Int): List<QuoteV2> = transaction {
        val quotes = mutableSetOf<QuoteV2>()
        val max = getTotal()

        while (quotes.count() < count) {
            quotes.addAll(QuoterV2.select { QuoterV2.id eq Random.nextInt(max) }.map(::QuoteV2))
        }
        quotes.toList()
    }

    fun getAll(): List<QuoteV2> = transaction {
        QuoterV2.selectAll().map(::QuoteV2)
    }

    fun isExists(id: Int): Boolean = transaction {
        QuoterV2.select { QuoterV2.id eq id }.count() > 0
    }

    fun addAttachment(id: Int, attachment: String) = transaction {
        val oldAttachments = mutableListOf(*QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2).first().attachments.split(";").toTypedArray())
        oldAttachments.add(attachment)
        val newAttachments = oldAttachments.joinToString(";")
        QuoterV2.update({ QuoterV2.id eq id }) { it[QuoterV2.attachments] = newAttachments }
    }

    suspend fun <T: Any> PipelineContext<Unit, ApplicationCall>.handle(func: () -> T, respond: (T) -> Any = { it }) {
        try {
            val result = func()
            if (result is List<*> && result.isEmpty()) {
                return call.respond(HttpStatusCode.NotFound)
            }

            call.respond(respond(result))
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    fun Route.quoterV2() = route("quote-v2") {

        put("/") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

            if (!Core.verifyKey(key)) {
                return@put call.respond(HttpStatusCode.Unauthorized)
            }

            val params = call.receiveParameters()

            val adder = params["adder"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val author = params["author"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val content = params["content"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val attachments = params["attachments"]?.split(";") ?:
                    emptyList()
             try {
                addQuote(adder, author, content, attachments)
                call.respond(HttpStatusCode.OK)
            } catch(e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        put("attach") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

            if (!Core.verifyKey(key)) {
                return@put call.respond(HttpStatusCode.Unauthorized)
            }

            val params = call.receiveParameters()
            val id = params["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val attachment = params["attachment"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest)

            val quoteExists = isExists(id)
            val attachmentExists = AttachmentsApi.isExists(attachment)

            if (!quoteExists) return@put call.respond(HttpStatusCode.NotFound)
            if (!attachmentExists) return@put call.respond(HttpStatusCode.NoContent)

            try {
                addAttachment(id, attachment)
            } catch (e: Exception) {
                e.printStackTrace()
                return@put call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            handle({ getById(id) }) { it.first() }
        }

        get("{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val to = call.parameters["to"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            handle({ getRange(from, to) })
        }

        get("random/{count?}") {
            val count = (call.parameters["count"] ?: "1").toIntOrNull() ?:
                    return@get call.respond(HttpStatusCode.BadRequest)

            handle({ getRandom(count) })
        }

        get("all") { handle(::getAll) }

        get("total") { handle(::getTotal) }
    }

}