package api

import config
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.gt22.uadam.data.Album
import com.gt22.uadam.data.BaseData
import com.gt22.uadam.data.MusicContext
import com.gt22.uadam.utils.str
import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import utils.ImATeapot
import utils.json
import java.nio.file.Paths


object MusicApi {

    private val musicContext = try {
        MusicContext.create(Paths.get(config["music_dir"]!!.str))
    } catch (e: Exception) {
        null
    }

    fun Route.music() = route("music") {
        get("/") {
            if (musicContext == null) {
                return@get call.respond(ImATeapot)
            }
            call.respond(jsonifyBaseData(musicContext))
        }
        get("/{path...}") {
            if (musicContext == null) {
                return@get call.respond(ImATeapot)
            }
            val path = call.parameters.getAll("path")?.joinToString(separator = "/") ?: return@get call.respond(BadRequest)
            call.respond(musicContext.search(path).map(MusicApi::jsonifyBaseData))
        }
    }

    private fun jsonifyBaseData(data: BaseData<*>): JsonElement = json {
        "type" to data::class.java.simpleName
        "meta" to getMeta(data)
        if (data is Album) {
            "children" to data.children.values.asSequence().map(BaseData<*>::name).map(::JsonPrimitive)
        } else {
            if (data.children.isNotEmpty()) {
                "children" to {
                    data.children.forEach { name, value -> name to jsonifyBaseData(value) }
                }
            }
        }
    }

    private fun getMeta(data: BaseData<*>) = json {
        "title" to data.title
        if (data.format != data.parent?.format) "format" to data.format
        if (data.img != data.parent?.img) "img" to data.img
    }

    private fun buildResponse(error: Boolean = false, errorMsg: String = "") = json {
        "response" to {
            "error" to error
            if (error) {
                "error_msg" to errorMsg
            }
        }
    }

}