package bakend.api

import bakend.verifyKey
import com.google.common.hash.Hashing
import bakend.dao.Attachments
import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
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

    private fun putAttachment(id: String, type: String, data: ByteArray): Boolean {
        return transaction {
            if (Attachments.select { Attachments.id eq id }.count() == 0) {
                val blob = connection.createBlob()
                blob.setBytes(1, data)
                Attachments.insert {
                    it[Attachments.id] = id
                    it[Attachments.type] = type
                    it[Attachments.data] = blob
                }
                true
            } else {
                false
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
            val key = call.request.header("X-Access-Key")
                    ?: return@put call.respond(Unauthorized)
            if (!verifyKey(key)) {
                return@put call.respond(Forbidden)
            }

            try {
                val type = call.request.header("X-Attachment-Content-Type")
                        ?: return@put call.respond(BadRequest)
                val data = call.receiveStream().readBytes()
                val id = Hashing.hmacSha256(type.toByteArray(StandardCharsets.UTF_8)).hashBytes(data).toString()
                val added = putAttachment(id, type, data)
                call.respond(if(added) Created else OK, id)
            } catch (e: Exception) {
                call.respond(InternalServerError)
            }
        }

        get("{id}") {
            val id = call.parameters["id"]
                    ?: return@get call.respond(BadRequest)

            val result = getAttachment(id)
                    ?: return@get call.respond(NotFound)


            call.response.header("X-Attachment-Content-Type", result.first)
            call.respond(result.second.binaryStream.readBytes())
        }

        delete("{id}") {
            val key = call.request.header("X-Access-Key")
                    ?: return@delete call.respond(Unauthorized)

            if (!verifyKey(key)) {
                return@delete call.respond(Forbidden)
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