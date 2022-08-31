package bakend.web

import bakend.api.QuoterApi
import bakend.dao.Quoter
import kotlinx.html.*
import bakend.model.Quote
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object QuoterWeb {

    fun Route.quoterWeb() = route("quote") {
        get("/{pos}") {
            val pos = call.parameters["pos"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
            try {
                val result = QuoterApi.getByPos(pos)
                if (result.isEmpty())  {
                    return@get call.respond(HttpStatusCode.NotFound)
                }

                call.respondHtml {
                    head {
                        title { +"Quote #$pos" }
                        link(rel = "stylesheet", href = "/static/styles.css", type = "text/css")
                    }
                    body {
                        div("quotes") {
                            for (quote in result) {
                                quote(quote)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                return@get call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/{pos}/edit") {
            val pos = call.parameters["pos"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
            try {
                val result = transaction {
                    Quoter.select { Quoter.id eq pos }.map(::Quote)
                }
                if (result.isEmpty())  {
                    return@get call.respond(HttpStatusCode.NotFound)
                }

                val quote = result[0]

                call.respondHtml {

                }
            } catch (e: Exception) {
                return@get call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    @HtmlTagMarker
    fun FlowContent.quote(quote: Quote) {
        div("quote"){
            div("quote-header") {
                div("quote-id"){
                    +quote.id.toString()
                }
                div("quote-author") {
                    +quote.author
                }
            }
            div("quote-text") {
                +quote.quote
            }
        }
    }


}