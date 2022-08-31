package bakend.api

import bakend.dao.Users
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import bakend.utils.json
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.SQLException


object ITHApi {

    fun Route.ith() = route("ith") {
        get("login/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(BadRequest)
            val storyId: Int = transaction {
                val id = Users.slice(Users.story).select { Users.user like username }.firstOrNull()?.get(Users.story)
                if (id == null) {
                    Users.insert { u ->
                        u[user] = username
                        u[story] = 1
                    }
                    return@transaction 1
                }
                return@transaction id
            }
            call.respond(json {
                "username" to username
                "storyId" to storyId
            })
        }
        post("set") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respond(BadRequest)
            val userId = params["storyId"]?.toIntOrNull() ?: return@post call.respond(BadRequest)
            try {
                transaction {
                    Users.update({ Users.user eq username }) { u ->
                        u[story] = userId
                    }
                }
                call.respond(OK)
            }catch (e: SQLException) {
                call.respond(InternalServerError)
            }
        }
    }
}