package bakend.api.quoterv2

import bakend.api.AttachmentsApi
import bakend.api.AttachmentsApi.attachments
import bakend.api.quoterv2.resolvers.IQuoterV2APIResolver
import bakend.api.quoterv2.resolvers.ResolverRegistry
import bakend.dao.getTable
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
import bakend.utils.ImATeapot
import bakend.utils.StatusCodeException
import bakend.verifyKey
import io.ktor.http.Parameters
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException

typealias Ctx = PipelineContext<Unit, ApplicationCall>

object QuoterV2Api {

    enum class AttachmentResult {
        Attached,
        AlreadyAttached,
        Error
    }


    private data class RequestCtx(val params: Parameters, val resolver: IQuoterV2APIResolver)

    private suspend fun <T : Any> Ctx.handle(func: suspend (RequestCtx) -> T, check: Boolean = true, respond: ((T) -> Any)? = { it }) {
        try {
            val params = if (call.request.httpMethod == HttpMethod.Get) call.parameters else call.receiveParameters()
            val result = func(RequestCtx(params, getResolver(params["resolver"])))
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

    private suspend fun Ctx.requestNewRepo() {
        val key = call.request.header("X-Access-Key")
                ?: return call.respond(Unauthorized)

        if (!verifyKey(key)) {
            return call.respond(Forbidden)
        }

        val params = call.receiveParameters()

        val name = params["name"]
                ?: return call.respond(BadRequest)

        try {
            val newTable = transaction { getTable(name, true) }

            if (newTable == null) {
                call.respond(ImATeapot)
            } else {
                call.respond(OK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }

    }

    private suspend fun Ctx.requestAdd(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val key = call.request.header("X-Access-Key")
                ?: return call.respond(Unauthorized)

        if (!verifyKey(key)) {
            return call.respond(Forbidden)
        }

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
            resolver.add(adder, authors, displayType, content, attachments)
            call.respond(OK)
        } catch (e: SQLException) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.requestAttach(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val key = call.request.header("X-Access-Key")
                ?: return call.respond(Unauthorized)

        if (!verifyKey(key)) {
            return call.respond(Forbidden)
        }

        val id = params["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val attachment = params["attachment"]
                ?: return call.respond(BadRequest)

        val quoteExists = resolver.exists(id)
        val attachmentExists = AttachmentsApi.exists(attachment)

        if (!quoteExists) return call.respond(NotFound)
        if (!attachmentExists) return call.respond(Gone)

        return try {
            when (resolver.attach(id, attachment)) {
                QuoterV2Api.AttachmentResult.Attached -> call.respond(OK)
                QuoterV2Api.AttachmentResult.AlreadyAttached -> call.respond(Conflict)
                QuoterV2Api.AttachmentResult.Error -> call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.requestGet(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val id = params["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        if(!resolver.exists(id)) {
            return call.respond(NotFound)
        }
        handle({ resolver.byId(id) })
    }

    private suspend fun Ctx.requestFromTo(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val from = params["from"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val to = params["to"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        if (from > to) return call.respond(BadRequest)

        handle({ resolver.range(from, to) }, check = false)
    }

    private suspend fun Ctx.requestRandom(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val count = (params["count"] ?: "1").toIntOrNull() ?: return call.respond(BadRequest)
        if (count < 0) return call.respond(BadRequest)

        handle({ resolver.random(count) }, check = false)
    }

    private suspend fun Ctx.proceedEdit(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val key = call.request.header("X-Access-Key")
                ?: return call.respond(Unauthorized)

        if (!verifyKey(key)) {
            return call.respond(Forbidden)
        }

        val id = params["id"]?.toIntOrNull()
                ?: return call.respond(BadRequest)
        val editedBy = params["edited_by"]
                ?: return call.respond(BadRequest)
        val newContent = params["new_content"]
                ?: return call.respond(BadRequest)

        val quoteExists = resolver.exists(id)
        if(!quoteExists) return call.respond(NotFound)

        try {
            return if (resolver.edit(id, editedBy, System.currentTimeMillis(), newContent)) {
                call.respond(OK)
            } else {
                call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.proceedFixIds(ctx: RequestCtx) {
        val (_, resolver) = ctx
        val key = call.request.header("X-Access-Key")
                ?: return call.respond(Forbidden)

        if (!verifyKey(key)) {
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

        attachments()

        /**
         * <b>Arguments</b>
         *  * adder - string - adder name (required)
         *  * authors - string - authors names separated by ; (required)
         *  * dtype - string - display type: dialog or text (text by default)
         *  * content - string - quote's content
         *  * attachments - string - list of attachments' ids separated by ;
         *
         * <b>Headers</b>
         *  * X-Access-Key - access key for UADAF API (required)
         *
         * <b>Response codes</b>
         *  * Forbidden - access key not passed
         *  * Unauthorized - access key is invalid
         *  * BadRequest - necessary params not passed
         *  * OK - added
         *  * ISE - exception
         */
        put("/") {
            handle({ requestAdd(it) }, respond = null)
        }

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
        get("all") { handle({ it.resolver.all() }, check = false) }

        /**
         * OK - succeed
         * ISE - exception
         */
        get("total") { handle({ it.resolver.total() }) }

        /**
         * NotFound - quote doesn't exists
         * Forbidden - if access key not passed
         * Unauthorized - if access key is invalid
         * OK - succeed
         * ISE - exception
         */
        post("edit") { handle({ proceedEdit(it) }, respond = null) }

        post("fix_ids") { handle({ proceedFixIds(it) }, respond = null) }

        put("repo") { requestNewRepo() }
    }

}