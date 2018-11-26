package api

import com.google.common.hash.Hashing
import dao.Quoter
import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import model.Quote
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.Date
import kotlin.random.Random

object QuoterApi {

    fun getByPos(pos: Int): List<Quote> = transaction {
        Quoter.select { Quoter.id eq pos }.map(::Quote)
    }

    fun getTotal(): Int = Quoter.selectAll().count()

    fun getRandom(count: Int): List<Quote> = transaction {
        val quotes = mutableSetOf<Quote>()
        val max = getTotal()
        while (quotes.count() < count) {
            quotes.addAll(Quoter.select {
                Quoter.id eq Random.nextInt(max)
            }.map(::Quote))
        }
        quotes.toList()
    }

    fun getRange(from: Int, to: Int): List<Quote> = transaction {
        Quoter.select {
            (Quoter.id greaterEq from) and (Quoter.id lessEq to)
        }.map(::Quote)
    }

    fun getAll(): List<Quote> = transaction {
        Quoter.selectAll().map(::Quote)
    }

    fun addQuote(adder: String, author: String, quote: String): Unit = transaction {
        Quoter.insert {
            it[Quoter.adder] = adder
            it[Quoter.author] = author
            it[Quoter.quote] = quote
        }
    }

    fun editQuote(id: Int, editedBy: String, editedAt: Long, newText: String): Boolean = transaction {
        Quoter.update({ Quoter.id eq id }) {
            it[Quoter.editedBy] = editedBy
            it[Quoter.quote] = newText
            it[Quoter.editedAt] = editedAt
        }
    } != 0

    fun Route.quoter() = route("quote") {
        get("pos/{pos}") {
            val pos = call.parameters["pos"]?.toIntOrNull() ?: return@get call.respond(BadRequest)
            try {
                val result = getByPos(pos)
                if (result.isEmpty())
                    return@get call.respond(NotFound)

                call.respond(result[0])
            } catch (e: Exception) {
                return@get call.respond(InternalServerError)
            }
        }

        get("random/{count?}") {
            val count = Math.abs(call.parameters["count"]?.toIntOrNull() ?: 1)
            try {
                val quotes = getRandom(count)
                call.respond(quotes)
            }catch (e: Exception) {
                return@get call.respond(InternalServerError)
            }
        }

        get("range/{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull() ?: return@get call.respond(NotFound)
            val to = call.parameters["to"]?.toIntOrNull() ?: return@get call.respond(NotFound)

            try {
                val quotes = getRange(from, to)
                call.respond(quotes)
            }catch (e: Exception) {
                call.respond(InternalServerError)
            }
        }

        get("total") {
            try {
                call.respond(getTotal())
            }catch (e: Exception) {
                call.respond(InternalServerError)
            }
        }

        get("all") {
            try {
                val quotes = getAll()
                call.respond(quotes)
            }catch (e: Exception) {
                return@get call.respond(InternalServerError)
            }
        }

        post("add") {
            val params = call.receiveParameters()
            val key = params["key"] ?: return@post call.respond(BadRequest)
            val adder = params["adder"] ?: return@post call.respond(BadRequest)
            val author = params["author"] ?: return@post call.respond(BadRequest)
            val quote = params["quote"] ?: return@post call.respond(BadRequest)

            if (!isKeyValid(key)) {
                return@post call.respond(Unauthorized)
            }
            try {
                addQuote(adder, author, quote)
                call.respond(OK)
            } catch(e: SQLException) {
                call.respond(InternalServerError)
            }
        }

        post("edit") {
            val params = call.receiveParameters()
            val key = params["key"] ?: return@post call.respond(BadRequest)
            val id = params["id"]?.toIntOrNull() ?: return@post call.respond(BadRequest)
            val editedBy = params["edited_by"] ?: return@post call.respond(BadRequest)
            val newText = params["new_text"] ?: return@post call.respond(BadRequest)

            if (!isKeyValid(key)) {
                return@post call.respond(Unauthorized)
            }

            try {
                val result = editQuote(id, editedBy, Date().time, newText)
                if (result) {
                    call.respond(OK)
                } else {
                    call.respond(NotFound)
                }
            } catch(e: SQLException) {
                call.respond(InternalServerError)
            }
        }
    }

//    private fun buildQuote(quote: Quote) = json {
//        "id" to quote.id
//        "adder" to quote.adder
//        "author" to quote.author
//        "quote" to quote.quote
//        "edited_by" to quote.editedBy
//        "edited_at" to quote.editedAt
//    }

//    private fun buildQuoteSequence(list: List<Quote>) = list.map(::buildQuote)

    private fun isKeyValid(key: String) =
            Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
                    "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"

}