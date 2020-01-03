package bakend.api.quoterv2

import bakend.api.AttachmentsApi
import bakend.api.AttachmentsApi.attachments
import bakend.api.quoterv2.resolvers.IQuoterV2APIResolver
import bakend.api.quoterv2.resolvers.ResolverRegistry
import bakend.dao.getTable
import bakend.utils.ImATeapot
import bakend.utils.StatusCodeException
import bakend.utils.json
import bakend.verifyKey
import com.google.gson.JsonObject
import com.gt22.uadam.utils.contains
import com.gt22.uadam.utils.str
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
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException

typealias Ctx = PipelineContext<Unit, ApplicationCall>

object QuoterV2Api {

    enum class AttachmentResult {
        Attached,
        AlreadyAttached,
        Error
    }


    private data class RequestCtx(val params: JsonObject, val resolver: IQuoterV2APIResolver)

    fun Parameters.toJson(): JsonObject = json {
        forEach { s, list ->
            s to list.first()
        }
    }

    private suspend fun <T : Any> Ctx.handle(func: suspend (RequestCtx) -> T, check: Boolean = true, respond: ((T) -> Any)? = { it }) {
        try {
            val params = if (call.request.httpMethod == HttpMethod.Get) call.parameters.toJson() else call.receive()
            if ("resolver" !in params) {
                call.respond(BadRequest)
                return
            }
            val result = func(RequestCtx(params, getResolver(params["resolver"].str)))
            if (respond != null) {
                if (check && result is List<*> && result.isEmpty()) {
                    return call.respond(NotFound)
                }
                call.respond(respond(result))
            }
        } catch (e: StatusCodeException) {
            call.respond(e.code)
        } catch (e: IllegalArgumentException) {
            call.respond(BadRequest)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.verifyKey(): Boolean {
        val key = call.request.header("X-Access-Key")
        if (key == null) {
            call.respond(Unauthorized)
            return false
        }
        if (!verifyKey(key)) {
            call.respond(Forbidden)
            return false
        }
        return true
    }

    private suspend fun Ctx.requestNewRepo() {
        verifyKey()
        val params = call.receiveParameters()
        try {
            val name = requireNotNull(params["name"])

            val newTable = transaction { getTable(name, true) }

            if (newTable == null) {
                call.respond(ImATeapot)
            } else {
                call.respond(OK)
            }
        } catch (e: IllegalArgumentException) {
            call.respond(BadRequest)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }

    }

    private suspend fun Ctx.requestAdd(ctx: RequestCtx) {
        val (params, resolver) = ctx
        verifyKey()
        try {
            val adder = requireNotNull(params["adder"]).str
            val authors = requireNotNull(params["authors"]).str
            val displayType = params["dtype"]?.str ?: "text"
            val content = requireNotNull(params["content"]).str
            val attachments = params["attachments"]?.str?.split(";") ?: emptyList()

            require(displayType in setOf("text", "dialog"))

            resolver.add(adder, authors, displayType, content, attachments)
            call.respond(OK)
        } catch (e: SQLException) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.requestAttach(ctx: RequestCtx) {
        val (params, resolver) = ctx
        verifyKey()
        val id = requireNotNull(params["id"]?.str?.toIntOrNull())
        val attachment = requireNotNull(params["attachment"]).str

        val quoteExists = resolver.exists(id)
        val attachmentExists = AttachmentsApi.exists(attachment)

        if (!quoteExists) return call.respond(NotFound)
        if (!attachmentExists) return call.respond(Gone)

        return try {
            when (resolver.attach(id, attachment)) {
                AttachmentResult.Attached -> call.respond(OK)
                AttachmentResult.AlreadyAttached -> call.respond(Conflict)
                AttachmentResult.Error -> call.respond(NotFound)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(InternalServerError)
        }
    }

    private suspend fun Ctx.requestGet(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val id = requireNotNull(params["id"]?.str?.toIntOrNull())
        if (!resolver.exists(id)) {
            return call.respond(NotFound)
        }
        handle({ resolver.byId(id) })
    }

    private suspend fun Ctx.requestFromTo(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val from = requireNotNull(params["from"]?.str?.toIntOrNull())
        val to = requireNotNull(params["to"]?.str?.toIntOrNull())
        require(from <= to)

        handle({ resolver.range(from, to) }, check = false)
    }

    private suspend fun Ctx.requestRandom(ctx: RequestCtx) {
        val (params, resolver) = ctx
        val count = requireNotNull((params["count"]?.str ?: "1").toIntOrNull())
        require(count >= 0)

        handle({ resolver.random(count) }, check = false)
    }

    private suspend fun Ctx.proceedEdit(ctx: RequestCtx) {
        val (params, resolver) = ctx
        verifyKey()
        val id = requireNotNull(params["id"]?.str?.toIntOrNull())
        val editedBy = requireNotNull(params["edited_by"]).str
        val newContent = requireNotNull(params["new_content"]).str

        val quoteExists = resolver.exists(id)
        if (!quoteExists) return call.respond(NotFound)
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
        verifyKey()

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