package api

import com.mysql.jdbc.SQLError
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import model.Quote
import mysql.Quoter
import mysql.Quoter.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Instances.gson
import utils.array
import utils.json
import java.sql.SQLException
import kotlin.random.Random

object QuoterApi {

    fun Route.quoter() =
        route("quote") {
            println("Hello, World, I'm quoter")
            get("pos/{pos}") {
                val pos = call.parameters["pos"]?.toIntOrNull() ?: return@get call.respond(gson.toJson(buildResponse(true, "INVALID_PARAMS")))
                call.respond(gson.toJson(buildResponse(quotesPayload = transaction {
                    Quoter.select {
                        id eq pos
                    }.map(::Quote)
                })))
            }

            get("random/{count?}") {
                val count = Math.abs(call.parameters["count"]?.toIntOrNull() ?: 1)
                val quotes = mutableListOf<Quote>()
                transaction {
                    val max = Quoter.selectAll().count()
                    while (quotes.count() < count) {
                        quotes.addAll(Quoter.select {
                            id eq Random.nextInt(max)
                        }.map(::Quote))
                    }
                }
                call.respond(gson.toJson(buildResponse(quotesPayload = quotes)))
            }

            get("range/{from}/{to}") {
                val from = call.parameters["from"]?.toIntOrNull()
                val to = call.parameters["to"]?.toIntOrNull()

                if (from == null || to == null) {
                    return@get call.respond(gson.toJson(buildResponse(true, "INVALID_PARAMS")))
                }
                call.respond(gson.toJson(buildResponse(quotesPayload = transaction {
                    Quoter.select {
                        (id greaterEq from) and (id less to)
                    }.map(::Quote)
                })))
            }

            get("total") {
                call.respond(transaction {
                    Quoter.selectAll().count().toString()
                })
            }

            get("all") {
                call.respond(gson.toJson(buildResponse(quotesPayload = transaction {
                    Quoter.selectAll().map(::Quote)
                })))
            }

            post("add") {
                val params = call.receiveParameters()
                val adder = params["adder"]
                val author = params["author"]
                val quote = params["quote"]

                if (adder == null || author == null || quote == null) {
                    return@post call.respond(gson.toJson(buildResponse(true, "INVALID_PARAMS")))
                }
                try {
                    transaction {
                        Quoter.insert {
                            it[Quoter.adder] = adder
                            it[Quoter.author] = author
                            it[Quoter.quote] = quote
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } catch(e: SQLException) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            post("edit") {
                val params = call.receiveParameters()
                val adder = params["adder"]
                val author = params["author"]
                val quote = params["quote"]

                if (adder == null || author == null || quote == null) {
                    return@post call.respond(gson.toJson(buildResponse(true, "INVALID_PARAMS")))
                }
                try {
                    transaction {
                        Quoter.insert {
                            it[Quoter.adder] = adder
                            it[Quoter.author] = author
                            it[Quoter.quote] = quote
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } catch(e: SQLException) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }


        }

    private fun buildQuote(quote: Quote) = json {
        "id" to quote.id
        "adder" to quote.adder
        "author" to quote.author
        "text" to quote.text
        "edited_by" to quote.editedBy
        "edited_at" to quote.editedAt
    }

    private fun buildQuoteSquence(list: List<Quote>) = list.map(::buildQuote).asSequence()

    private fun buildResponse(error: Boolean = false, errorMsg: String = "", quotesPayload: List<Quote> = emptyList()) = json {
        "response" to {
            "error" to error
            if (error) {
                "error_msg" to errorMsg
            } else {
                "payload" to array(buildQuoteSquence(quotesPayload))
            }
        }
    }

}