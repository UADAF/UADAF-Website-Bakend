package api

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.gt22.uadam.data.Album
import com.gt22.uadam.data.BaseData
import com.gt22.uadam.data.RemoteMusicContext
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import utils.Instances.gson
import utils.json
import java.net.URL

object MusicApi {

    private val musicContext = RemoteMusicContext.create(URL("http://52.48.142.75:8888/backend/music"), "UADAMusic")

    fun Route.music() = route("music") {
        get("/") { _ ->
            call.respond(gson.toJson(jsonifyBaseData(musicContext)))
        }
    }

    private fun jsonifyBaseData(data: BaseData): JsonElement = json {
        "meta" to getMeta(data)
        if (data is Album) {
            "children" to data.children.values.asSequence().map(BaseData::name).map(::JsonPrimitive)
        } else {
            "children" to {
                data.children.forEach { name, value -> name to jsonifyBaseData(value) }
            }
        }
    }

    private fun getMeta(data: BaseData) = json {
        if (data.title != data.name) "title" to data.title
        if (data.format != data.parent?.format) "format" to data.format
        if (data.img != data.parent?.img) "img" to data.img
    }

}