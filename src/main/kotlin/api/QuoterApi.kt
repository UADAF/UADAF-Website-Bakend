package api

import com.google.common.hash.Hashing
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import model.Quote
import mysql.Quoter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Instances.gson
import utils.array
import utils.json
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.Date
import kotlin.random.Random

object QuoterApi {

    fun getByPos(pos: Int): List<Quote> {
        return transaction {
            Quoter.select { Quoter.id eq pos }.map(::Quote)
        }
    }

    fun getTotal(): Int {
        return Quoter.selectAll().count()
    }

    fun getRandom(count: Int): List<Quote> {
        val quotes = mutableListOf<Quote>()
        transaction {
            val max = getTotal()
            while (quotes.count() < count) {
                quotes.addAll(Quoter.select {
                    Quoter.id eq Random.nextInt(max)
                }.map(::Quote))
            }
        }
        return quotes
    }

    fun getRange(from: Int, to: Int): List<Quote> {
        return transaction {
            Quoter.select {
                (Quoter.id greaterEq from) and (Quoter.id lessEq to)
            }.map(::Quote)
        }
    }

    fun getAll(): List<Quote> {
        return transaction {
            Quoter.selectAll().map(::Quote)
        }
    }

    fun addQuote(adder: String, author: String, quote: String) {
        transaction {
            Quoter.insert {
                it[Quoter.adder] = adder
                it[Quoter.author] = author
                it[Quoter.quote] = quote
            }
        }
    }

    fun editQuote(id: Int, editedBy: String, editedAt: Long, newText: String): Boolean {
        return transaction {
            Quoter.update({ Quoter.id eq id }) {
                it[Quoter.editedBy] = editedBy
                it[Quoter.quote] = newText
                it[Quoter.editedAt] = editedAt
            }
        } != 0
    }

    fun Route.quoter() = route("quote") {
        get("pos/{pos}") {
            val pos = call.parameters["pos"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            try {
                val result = getByPos(pos)
                if (result.isEmpty())
                    return@get call.respond(HttpStatusCode.NotFound)

                call.respond(gson.toJson(buildQuote(result[0])))
            } catch (e: Exception) {
                return@get call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("random/{count?}") {
            val count = Math.abs(call.parameters["count"]?.toIntOrNull() ?: 1)
            try {
                val quotes = getRandom(count)
                call.respond(gson.toJson(buildQuoteSequence(quotes)))
            }catch (e: Exception) {
                return@get call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("range/{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
            val to = call.parameters["to"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)

            try {
                val quotes = getRange(from, to)
                call.respond(gson.toJson(buildQuoteSequence(quotes)))
            }catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("total") {
            try {
                call.respond(getTotal())
            }catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("all") {
            try {
                val quotes = getAll()
                call.respond(gson.toJson(buildQuoteSequence(quotes)))
            }catch (e: Exception) {
                return@get call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("add") { _ ->
            val params = call.receiveParameters()
            val key = params["key"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val adder = params["adder"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val author = params["author"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val quote = params["quote"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            if (!isKeyValid(key)) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }
            try {
                addQuote(adder, author, quote)
                call.respond(HttpStatusCode.OK)
            } catch(e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("edit") { _ ->
            val params = call.receiveParameters()
            val key = params["key"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val id = params["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val editedBy = params["edited_by"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val newText = params["new_text"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            if (!isKeyValid(key)) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }

            try {
                val result = editQuote(id, editedBy, Date().time, newText)
                if (result) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch(e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    private fun buildQuote(quote: Quote) = json {
        "id" to quote.id
        "adder" to quote.adder
        "author" to quote.author
        "quote" to quote.quote
        "edited_by" to quote.editedBy
        "edited_at" to quote.editedAt
    }

    private fun buildQuoteSequence(list: List<Quote>) =
            array(list.map(::buildQuote).asSequence())

    private fun isKeyValid(key: String) =
            Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
                    "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"

}