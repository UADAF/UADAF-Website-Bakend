package api

import dao.QuoterV2
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import model.QuoteV2
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Integer.min
import java.sql.SQLException
import kotlin.random.Random

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Gone
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized

object QuoterV2Api {

    fun addQuote(adderIn: String, authorsIn: String, displayTypeIn: String, contentIn: String, attachmentsIn: List<String>): Unit = transaction {
        QuoterV2.insert {
            it[adder] = adderIn
            it[authors] = authorsIn
            it[content] = contentIn
            it[dtype] = displayTypeIn
            it[attachments] = attachmentsIn.joinToString(";")
        }
    }

    fun getById(id: Int): List<QuoteV2> = transaction {
        QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2)
    }

    fun getRange(from: Int, to: Int): List<QuoteV2> = transaction {
        QuoterV2.select { (QuoterV2.id greaterEq from) and (QuoterV2.id lessEq to) }.map(::QuoteV2)
    }

    fun getTotal(): Int = transaction { QuoterV2.selectAll().count() }

    fun getRandom(c: Int): List<QuoteV2> = transaction {
        val total =  QuoterV2.selectAll().count()
        val count = min(c, total)
        val indexes = (0..total).toMutableList()

        for (i in 0..(total - count)) {
            indexes.removeAt(Random.nextInt(indexes.size))
        }

        return@transaction QuoterV2.select { QuoterV2.id inList indexes }.map(::QuoteV2)
    }

    fun fixIds() = transaction {
        val allIds = QuoterV2.slice(QuoterV2.id).selectAll().map { it[QuoterV2.id] }

        allIds.forEachIndexed { index, id ->
            if (index + 1 != id) {
                QuoterV2.update({
                    QuoterV2.id eq id
                }) {
                    it[QuoterV2.id] = index + 1
                }
            }
        }
    }

    fun getAll(): List<QuoteV2> = transaction {
        QuoterV2.selectAll().map(::QuoteV2)
    }

    fun isExists(id: Int): Boolean = transaction {
        QuoterV2.select { QuoterV2.id eq id }.count() > 0
    }

    enum class AttachmentResult {
        Attached,
        AlreadyAttached,
        Error
    }
    fun addAttachment(id: Int, attachment: String): AttachmentResult = transaction {
        val oldAttachments = QuoterV2.select { QuoterV2.id eq id }.map(::QuoteV2).first().attachments.toTypedArray()
        if (attachment in oldAttachments) return@transaction AttachmentResult.AlreadyAttached
        val newAttachments = listOf(*oldAttachments, attachment).joinToString(";")

        return@transaction if (QuoterV2.update({ QuoterV2.id eq id }) { it[attachments] = newAttachments } != 0) {
            AttachmentResult.Attached
        } else {
            AttachmentResult.Error
        }
    }

    fun editQuote(idIn: Int, editedByIn: String, editedAtIn: Long, newContentIn: String) = transaction {
        val oldContent = QuoterV2.select { QuoterV2.id eq idIn }.map(::QuoteV2).first().content
        QuoterV2.update({ QuoterV2.id eq idIn }) {
            it[editedBy] = editedByIn
            it[editedAt] = editedAtIn
            it[previousContent] = oldContent
            it[content] = newContentIn
        } != 0
    }

    suspend fun <T: Any> PipelineContext<Unit, ApplicationCall>.handle(func: () -> T, check: Boolean = true ,respond: (T) -> Any = { it }) {
        try {
            val result = func()
            if (check && result is List<*> && result.isEmpty()) {
                return call.respond(NotFound)
            }

            call.respond(respond(result))
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    fun Route.quoterV2() = route("quote") {

        /**
         * <b>Arguments</b>
         *  * adder - string - adder name (required)
         *  * authors - string - authors names separated by ; (required)
         *  * dtype - string - display type: dialog or text (text by default)
         *  * content - string - quote's content
         *  * attachments - string - list of attachments' ids separated by ;
         *
         * <b>Headers</b>
         *  * Access-Key - access key for UADAF API (required)
         *
         * <b>Response codes</b>
         *  * Forbidden - if access key not passed
         *  * Unauthorized - if access key is invalid
         *  * BadRequest - necessary params not passed
         *  * OK - added
         *  * ISE - exception
         */
        put("/") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(Forbidden)

            if (!Core.verifyKey(key)) {
                return@put call.respond(Unauthorized)
            }

            val params = call.receiveParameters()

            val adder = params["adder"]
                    ?: return@put call.respond(BadRequest)
            val authors = params["authors"]
                    ?: return@put call.respond(BadRequest)
            val displayType = params["dtype"]
                    ?: "text"
            val content = params["content"]
                    ?: return@put call.respond(BadRequest)
            val attachments = params["attachments"]?.split(";") ?:
                    emptyList()

            if (displayType !in setOf("text", "dialog"))
                return@put call.respond(BadRequest)

             try {
                addQuote(adder, authors, content, displayType, attachments)
                call.respond(OK)
            } catch(e: SQLException) {
                call.respond(InternalServerError)
            }
        }

        /**
        * Forbidden - if access key not passed
        * Unauthorized - if access key is invalid
        * BadRequest - necessary params not passed
        * OK - attached
        * NotFound - quote doesn't exists
        * Gone - attachment doesn't exists
        * Conflict - already attached
        * ISE - exception
        */
        put("attach") {
            val key = call.request.header("Access-Key")
                    ?: return@put call.respond(Forbidden)

            if (!Core.verifyKey(key)) {
                return@put call.respond(Unauthorized)
            }

            val params = call.receiveParameters()
            val id = params["id"]?.toIntOrNull()
                    ?: return@put call.respond(BadRequest)
            val attachment = params["attachment"]
                    ?: return@put call.respond(BadRequest)

            val quoteExists = isExists(id)
            val attachmentExists = AttachmentsApi.isExists(attachment)

            if (!quoteExists) return@put call.respond(NotFound)
            if (!attachmentExists) return@put call.respond(Gone)

            try {
                return@put when (addAttachment(id, attachment)) {
                    QuoterV2Api.AttachmentResult.Attached           -> call.respond(OK)
                    QuoterV2Api.AttachmentResult.AlreadyAttached    -> call.respond(Conflict)
                    QuoterV2Api.AttachmentResult.Error              -> call.respond(NotFound)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@put call.respond(InternalServerError)
            }
        }

        /**
         * BadRequest - id not passed or invalid
         * NotFound - quote not found
         * OK - succeed
         * ISE - exception
         */
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(BadRequest)

            handle({ getById(id) }) { it.first() }
        }

        /**
         * BadRequest - if params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull()
                    ?: return@get call.respond(BadRequest)
            val to = call.parameters["to"]?.toIntOrNull()
                    ?: return@get call.respond(BadRequest)
            if (from > to) return@get call.respond(BadRequest)

            handle({ getRange(from, to) }, check=false)
        }

        /**
         * BadRequest - if params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("random/{count?}") {
            val count = (call.parameters["count"] ?: "1").toIntOrNull() ?:
                    return@get call.respond(BadRequest)
            if (count < 0) return@get call.respond(BadRequest)

            handle({ getRandom(count) }, check=false)
        }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("all") { handle(::getAll, check=false) }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("total") { handle(::getTotal) }

        /**
         * NotFound - quote doesn't exists
         * Forbidden - if access key not passed
         * Unauthorized - if access key is invalid
         * OK - succeed
         * ISE - exception
         */
        post("edit") {
            val key = call.request.header("Access-Key")
                    ?: return@post call.respond(Forbidden)

            if (!Core.verifyKey(key)) {
                return@post call.respond(Unauthorized)
            }

            val params = call.receiveParameters()
            val id = params["id"]?.toIntOrNull()
                    ?: return@post call.respond(BadRequest)
            val editedBy = params["edited_by"]
                    ?: return@post call.respond(BadRequest)
            val newContent = params["new_content"]
                    ?: return@post call.respond(BadRequest)

            try {
                if (editQuote(id, editedBy, System.currentTimeMillis(), newContent)) {
                    return@post call.respond(OK)
                } else {
                    return@post call.respond(NotFound)
                }
            } catch(e: Exception) {
                e.printStackTrace()
                call.respond(InternalServerError)
            }
        }

        post("fix_ids") {
            val key = call.request.header("Access-Key")
                    ?: return@post call.respond(Forbidden)

            if (!Core.verifyKey(key)) {
                return@post call.respond(Unauthorized)
            }

            try {
                fixIds()
                return@post call.respond(OK)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(InternalServerError)
            }
        }
    }

}