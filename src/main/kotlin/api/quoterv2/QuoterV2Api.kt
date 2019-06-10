package api.quoterv2

import Core
import api.AttachmentsApi
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Gone
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import model.QuoteV2
import java.sql.SQLException

object QuoterV2Api {

    enum class AttachmentResult {
        Attached,
        AlreadyAttached,
        Error
    }

    suspend fun <T: Any> PipelineContext<Unit, ApplicationCall>.handle(func: () -> T, check: Boolean = true, respond: (T) -> Any = { it }) {
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

    suspend fun PipelineContext<Unit, ApplicationCall>.requestAdd(payloadFunction: (String, String, String, String, List<String>) -> Unit) {
        val key = call.request.header("Access-Key")
                ?: return call.respond(Forbidden)

        if (!Core.verifyKey(key)) {
            return call.respond(Unauthorized)
        }

        val params = call.receiveParameters()

        val adder = params["adder"]
                ?: return call.respond(BadRequest)
        val authors = params["authors"]
                ?: return call.respond(BadRequest)
        val displayType = params["dtype"]
                ?: "text"
        val content = params["content"]
                ?: return call.respond(BadRequest)
        val attachments = params["attachments"]?.split(";") ?:
        emptyList()

        if (displayType !in setOf("text", "dialog"))
            return call.respond(BadRequest)

        try {
            payloadFunction(adder, authors, content, displayType, attachments)
            call.respond(OK)
        } catch(e: SQLException) {
            call.respond(InternalServerError)
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.requestAttach(payloadFunction: (Int, String) -> AttachmentResult, checkFunction: (Int) -> Boolean) {
        val key = call.request.header("Access-Key")
                ?: return call.respond(Forbidden)

        if (!Core.verifyKey(key)) {
            return call.respond(Unauthorized)
        }

        val params = call.receiveParameters()
        val id = params["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val attachment = params["attachment"]
                ?: return call.respond(BadRequest)

        val quoteExists = checkFunction(id)
        val attachmentExists = AttachmentsApi.isExists(attachment)

        if (!quoteExists) return call.respond(NotFound)
        if (!attachmentExists) return call.respond(Gone)

        return try {
            when (payloadFunction(id, attachment)) {
                QuoterV2Api.AttachmentResult.Attached -> call.respond(OK)
                QuoterV2Api.AttachmentResult.AlreadyAttached -> call.respond(Conflict)
                QuoterV2Api.AttachmentResult.Error -> call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.requestGet(payloadFunction: (Int) -> List<QuoteV2>) {
        val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)

        handle({ payloadFunction(id) }) { it.first() }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.requestFromTo(payloadFunction: (Int, Int) -> List<QuoteV2>) {
        val from = call.parameters["from"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val to = call.parameters["to"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        if (from > to) return call.respond(BadRequest)

        handle({ payloadFunction(from, to) }, check=false)
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.requestRandom(payloadFunction: (Int) -> List<QuoteV2>) {
        val count = (call.parameters["count"] ?: "1").toIntOrNull() ?:
        return call.respond(BadRequest)
        if (count < 0) return call.respond(BadRequest)

        handle({ payloadFunction(count) }, check=false)
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.proceedEdit(payloadFunction: (Int, String, Long, String) -> Boolean) {
        val key = call.request.header("Access-Key")
                ?: return call.respond(Forbidden)

        if (!Core.verifyKey(key)) {
            return call.respond(Unauthorized)
        }

        val params = call.receiveParameters()
        val id = params["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val editedBy = params["edited_by"]
                ?: return call.respond(BadRequest)
        val newContent = params["new_content"]
                ?: return call.respond(BadRequest)

        try {
            return if (payloadFunction(id, editedBy, System.currentTimeMillis(), newContent)) {
                call.respond(OK)
            } else {
                call.respond(NotFound)
            }
        } catch(e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.proceedFixIds(payloadFunction: () -> Unit) {
        val key = call.request.header("Access-Key")
                ?: return call.respond(Forbidden)

        if (!Core.verifyKey(key)) {
            return call.respond(Unauthorized)
        }

        try {
            payloadFunction()
            return call.respond(OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    fun PipelineContext<Unit, ApplicationCall>.getResolver(): IQuoterV2APIResolver {
        return QuoterV2APIDatabaseResolver
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
        put("/") { requestAdd(getResolver()::addQuote) }

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
        put("attach") { with(getResolver()) { requestAttach(::addAttachment, ::isExists) } }

        /**
         * BadRequest - id not passed or invalid
         * NotFound - quote not found
         * OK - succeed
         * ISE - exception
         */
        get("{id}") { with(getResolver()) { requestGet(::getById) } }

        /**
         * BadRequest - if params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("{from}/{to}") { with(getResolver()) { requestFromTo(::getRange) } }

        /**
         * BadRequest - if params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("random/{count?}") { with(getResolver()) { requestRandom(::getRandom) } }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("all") { with(getResolver()) { handle(::getAll, check=false) } }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("total") { with(getResolver()) {  handle(::getTotal)  } }

        /**
         * NotFound - quote doesn't exists
         * Forbidden - if access key not passed
         * Unauthorized - if access key is invalid
         * OK - succeed
         * ISE - exception
         */
        post("edit") { with(getResolver()) {  proceedEdit(::editQuote) } }

        post("fix_ids") { with(getResolver()) { proceedFixIds(::fixIds) } }
    }

}