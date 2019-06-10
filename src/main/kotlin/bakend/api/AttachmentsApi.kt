package bakend.api

import bakend.verifyKey
import com.google.common.hash.Hashing
import bakend.dao.Attachments
import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.request.header
import io.ktor.request.receiveStream
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.sql.Blob


object AttachmentsApi {

    private fun putAttachment(id: String, type: String, data: ByteArray) {
        transaction {
            val blob = connection.createBlob()
            blob.setBytes(1, data)
            Attachments.insert {
                it[Attachments.id] = id
                it[Attachments.type] = type
                it[Attachments.data] = blob
            }
        }
    }

    private fun getAttachment(id: String): Pair<String, Blob>? {
        return transaction {
            val row = Attachments.select { Attachments.id eq id }.firstOrNull() ?: return@transaction null
            return@transaction row[Attachments.type].toString() to row[Attachments.data]
        }
    }

    fun exists(id: String): Boolean = transaction {
        Attachments.select { Attachments.id eq id }.count() > 0
    }

    fun Route.attachments() = route("attachments") {
        put("/") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(Forbidden)
            if (!verifyKey(key)) {
                return@put call.respond(Unauthorized)
            }

            try {
                val type = call.request.header("Content-Type")
                        ?: return@put call.respond(BadRequest)
                val data = call.receiveStream().readBytes()
                val id = Hashing.hmacSha256(System.currentTimeMillis().toString().toByteArray(StandardCharsets.UTF_8)).hashBytes(data).toString()
                putAttachment(id, type, data)
                call.response.header("A-ID", id)
                call.respond(Accepted)
            } catch (e: Exception) {
                call.respond(InternalServerError)
            }
        }

        get("{id}") {
            val key = call.request.header("Access-Key")
                    ?: return@get call.respond(Forbidden)

            if (!verifyKey(key)) {
                return@get call.respond(Unauthorized)
            }

            val id = call.parameters["id"]
                    ?: return@get call.respond(BadRequest)

            val result = getAttachment(id)
                    ?: return@get call.respond(NotFound)


            call.response.header("A-Content-Type", result.first)
            call.respond(result.second.binaryStream.readBytes())
        }

        delete("{id}") {
            val key = call.request.header("Access-Key")
                    ?: return@delete call.respond(Forbidden)

            if (!verifyKey(key)) {
                return@delete call.respond(Unauthorized)
            }

            val id = call.parameters["id"]
                    ?: return@delete call.respond(BadRequest)

            getAttachment(id)
                    ?: return@delete call.respond(NotFound)

            transaction {
                Attachments.deleteWhere { Attachments.id eq id }
            }
            call.respond(OK)
        }
    }

}