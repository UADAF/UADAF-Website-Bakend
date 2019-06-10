package api.quoterv2

import Core
import api.AttachmentsApi
import api.quoterv2.resolvers.ResolverRegistry
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Gone
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import utils.StatusCodeException
import java.sql.SQLException

typealias Ctx = PipelineContext<Unit, ApplicationCall>

object QuoterV2Api {

    enum class AttachmentResult {
        Attached,
        AlreadyAttached,
        Error
    }


    suspend fun <T : Any> Ctx.handle(func: suspend (IQuoterV2APIResolver) -> T, check: Boolean = true, respond: ((T) -> Any)? = { it }) {
        try {
            val params = if (call.request.httpMethod == HttpMethod.Get) call.parameters else call.receiveParameters()
            val result = func(getResolver(params["resolver"]))
            if (respond != null) {
                if (check && result is List<*> && result.isEmpty()) {
                    return call.respond(NotFound)
                }
                call.respond(respond(result))
            }
        } catch (e: StatusCodeException) {
            call.respond(e.code)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }


    suspend fun Ctx.requestAdd(resolver: IQuoterV2APIResolver) {
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
        val attachments = params["attachments"]?.split(";") ?: emptyList()

        if (displayType !in setOf("text", "dialog"))
            return call.respond(BadRequest)

        try {
            resolver.addQuote(adder, authors, content, displayType, attachments)
            call.respond(OK)
        } catch (e: SQLException) {
            call.respond(InternalServerError)
        }
    }

    suspend fun Ctx.requestAttach(resolver: IQuoterV2APIResolver) {
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

        val quoteExists = resolver.isExists(id)
        val attachmentExists = AttachmentsApi.isExists(attachment)

        if (!quoteExists) return call.respond(NotFound)
        if (!attachmentExists) return call.respond(Gone)

        return try {
            when (resolver.addAttachment(id, attachment)) {
                QuoterV2Api.AttachmentResult.Attached -> call.respond(OK)
                QuoterV2Api.AttachmentResult.AlreadyAttached -> call.respond(Conflict)
                QuoterV2Api.AttachmentResult.Error -> call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    suspend fun Ctx.requestGet(resolver: IQuoterV2APIResolver) {
        val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)

        handle({ resolver.getById(id) }) { it.first() }
    }

    suspend fun Ctx.requestFromTo(resolver: IQuoterV2APIResolver) {
        val from = call.parameters["from"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val to = call.parameters["to"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        if (from > to) return call.respond(BadRequest)

        handle({ resolver.getRange(from, to) }, check = false)
    }

    suspend fun Ctx.requestRandom(resolver: IQuoterV2APIResolver) {
        val count = (call.parameters["count"] ?: "1").toIntOrNull() ?: return call.respond(BadRequest)
        if (count < 0) return call.respond(BadRequest)

        handle({ resolver.getRandom(count) }, check = false)
    }

    suspend fun Ctx.proceedEdit(resolver: IQuoterV2APIResolver) {
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
            return if (resolver.editQuote(id, editedBy, System.currentTimeMillis(), newContent)) {
                call.respond(OK)
            } else {
                call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    suspend fun Ctx.proceedFixIds(resolver: IQuoterV2APIResolver) {
        val key = call.request.header("Access-Key")
                ?: return call.respond(Forbidden)

        if (!Core.verifyKey(key)) {
            return call.respond(Unauthorized)
        }

        try {
            resolver.fixIds()
            return call.respond(OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private fun getResolver(spec: String?): IQuoterV2APIResolver {
        try {
            return ResolverRegistry.getResolver(spec ?: "uadaf")
        } catch (e: NoSuchElementException) {
            throw StatusCodeException(BadRequest)
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
         *  * Forbidden - access key not passed
         *  * Unauthorized - access key is invalid
         *  * BadRequest - necessary params not passed
         *  * OK - added
         *  * ISE - exception
         */
        put("/") { handle({ requestAdd(it) }, respond = null) }

        /**
         * Forbidden - access key not passed
         * Unauthorized - access key is invalid
         * BadRequest - necessary params not passed
         * OK - attached
         * NotFound - quote doesn't exists
         * Gone - attachment doesn't exists
         * Conflict - already attached
         * ISE - exception
         */
        put("attach") { handle({ requestAttach(it) }, respond = null) }

        /**
         * BadRequest - id not passed or invalid
         * NotFound - quote not found
         * OK - succeed
         * ISE - exception
         */
        get("{id}") { handle({ requestGet(it) }, respond = null) }

        /**
         * BadRequest - params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("{from}/{to}") { handle({ requestFromTo(it) }, respond = null) }

        /**
         * BadRequest - params not passed or invalid
         * OK - succeed
         * ISE - exception
         */
        get("random/{count?}") { handle({ requestRandom(it) }, respond = null) }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("all") { handle({ it.getAll() }, check = false) }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("total") { handle({ it.getTotal() }) }

        /**
         * NotFound - quote doesn't exists
         * Forbidden - if access key not passed
         * Unauthorized - if access key is invalid
         * OK - succeed
         * ISE - exception
         */
        post("edit") { handle({ proceedEdit(it) }, respond = null) }

        post("fix_ids") { handle({ proceedFixIds(it) }, respond = null) }
    }

}