package api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import mysql.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import utils.Instances
import utils.Instances.gson
import utils.json
import java.sql.SQLException

object ITHApi {

    fun Route.ith() = route("ith") {
        get("login/{username}") {
            val username = call.parameters["username"] ?: return@get call.respond(Instances.gson.toJson(buildResponse(true, "INVALID_PARAMS")))
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
            call.respond(gson.toJson(buildResponse(payloadUsername = username, payloadStoryId = storyId)))
        }
        post("set") {
            val params = call.receiveParameters()
            val username = params["username"]
            val userId = params["storyId"]?.toIntOrNull()

            if (username == null || userId == null) {
                return@post call.respond(gson.toJson(buildResponse(true, "INVALID_PARAMS")))
            }
            try {
                transaction {
                    Users.update({ Users.user eq username }) { u ->
                        u[story] = userId
                    }
                }
                call.respond(HttpStatusCode.OK)
            }catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    private fun buildResponse(error: Boolean = false, errorMsg: String = "", payloadUsername: String = "", payloadStoryId: Int = 1) = json {
        "response" to {
            "error" to error
            if (error) {
                "error_msg" to errorMsg
            } else {
                "payload" to json {
                    "username" to payloadUsername
                    "storyId" to payloadStoryId
                }
            }
        }
    }
}