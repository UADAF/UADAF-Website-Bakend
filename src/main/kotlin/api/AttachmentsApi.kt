package api

import com.google.common.hash.Hashing
import dao.Attachments
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
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

    fun putAttachment(id: String, type: String, data: ByteArray) {
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

    fun getAttachment(id: String): Pair<String, Blob>? {
        return transaction {
            val rows = Attachments.select { Attachments.id eq id }
            if (rows.count() > 0) {
                val row = rows.firstOrNull() ?: return@transaction null

                return@transaction row[Attachments.type].toString() to row[Attachments.data]
            } else {
                return@transaction null
            }
        }
    }

    fun isExists(id: String): Boolean = transaction {
        Attachments.select { Attachments.id eq id }.count() > 0
    }

    fun Route.attachments() = route("attachments") {
        put("/") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            if (!Core.verifyKey(key)) {
                return@put call.respond(HttpStatusCode.Unauthorized)
            }

            try {
                val type = call.request.header("Content-Type")
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                val data = call.receiveStream().readBytes()
                val id = Hashing.hmacSha256(System.currentTimeMillis().toString().toByteArray(StandardCharsets.UTF_8)).hashBytes(data).toString()
                putAttachment(id, type, data)
                call.response.header("A-ID", id)
                call.respond(HttpStatusCode.Accepted)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        get("{id}") {
            val key = call.request.header("Access-Key")
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            if (!Core.verifyKey(key)) {
                return@get call.respond(HttpStatusCode.Unauthorized)
            }

            val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            val result = getAttachment(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)


            call.response.header("A-Content-Type", result.first)
            call.respond(result.second.binaryStream.readBytes())
        }

        delete("{id}") {
            val key = call.request.header("Access-Key")
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

            if (!Core.verifyKey(key)) {
                return@delete call.respond(HttpStatusCode.Unauthorized)
            }

            val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

            getAttachment(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

            transaction {
                Attachments.deleteWhere { Attachments.id eq id }
            }
            call.respond(HttpStatusCode.OK)
        }
    }

}