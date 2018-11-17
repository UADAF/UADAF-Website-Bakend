package api

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.gt22.uadam.data.Album
import com.gt22.uadam.data.BaseData
import com.gt22.uadam.data.MusicContext
import com.gt22.uadam.utils.str
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import utils.Instances
import utils.Instances.gson
import utils.json
import java.nio.file.Paths
import config.config

object MusicApi {

    private val musicContext = try {
        MusicContext.create(Paths.get(config["music_dir"]!!.str))
    } catch (e: Exception) {
        null
    }

    fun Route.music() = route("music") {
        get("/") {
            if (musicContext == null) {
                return@get call.respond(gson.toJson(buildResponse(true, "MUSIC_CONTEXT_NULL")))
            }
            call.respond(gson.toJson(jsonifyBaseData(musicContext)))
        }
        get("/{path...}") {
            if (musicContext == null) {
                return@get call.respond(gson.toJson(buildResponse(true, "MUSIC_CONTEXT_NULL")))
            }
            val path = call.parameters.getAll("path")?.joinToString(separator = "/") ?: return@get call.respond(Instances.gson.toJson(buildResponse(true, "INVALID_PARAMS")))
            call.respond(gson.toJson(musicContext.search(path).map(MusicApi::jsonifyBaseData)))
        }
    }

    private fun jsonifyBaseData(data: BaseData): JsonElement = json {
        "type" to data::class.java.simpleName
        "meta" to getMeta(data)
        if (data is Album) {
            "children" to data.children.values.asSequence().map(BaseData::name).map(::JsonPrimitive)
        } else {
            if (data.children.isNotEmpty()) {
                "children" to {
                    data.children.forEach { name, value -> name to jsonifyBaseData(value) }
                }
            }
        }
    }

    private fun getMeta(data: BaseData) = json {
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