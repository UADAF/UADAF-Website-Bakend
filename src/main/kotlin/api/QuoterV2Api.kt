package api

import dao.QuoterV2
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.put
import io.ktor.routing.route
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException

object QuoterV2Api {

    fun addQuote(adder: String, author: String, content: String, attachments: List<String>): Unit = transaction {
        QuoterV2.insert {
            it[QuoterV2.adder] = adder
            it[QuoterV2.author] = author
            it[QuoterV2.content] = content
            it[QuoterV2.attachments] = attachments.joinToString(";")
        }
    }

    fun Route.quoterV2() = route("quote/v2") {
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
    }

}