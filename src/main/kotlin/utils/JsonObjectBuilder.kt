package utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.gt22.uadam.utils.set

@DslMarker
annotation class JOBMarker

@JOBMarker
class JsonObjectBuilder {

    val obj = JsonObject()

    infix fun String.to(value: String) {
        obj[this] = value
    }

    infix fun String.to(value: Char) {
        obj[this] = value
    }

    infix fun String.to(value: Number) {
        obj[this] = value
    }

    infix fun String.to(value: Boolean) {
        obj[this] = value
    }

    infix fun String.to(value: JsonElement) {
        obj[this] = value
    }

    infix fun String.to(builder: JsonObjectBuilder.() -> Unit) {
        obj[this] = json(builder)
    }

    infix fun String.to(seq: Sequence<JsonElement>) {
        val arr = JsonArray()
        seq.forEach(arr::add)
        obj[this] = arr
    }
}

fun json(block: JsonObjectBuilder.() -> Unit): JsonObject {
    val b = JsonObjectBuilder()
    b.block()
    return b.obj
}

fun array(seq: Sequence<JsonElement>): JsonArray {
    val arr = JsonArray()
    seq.forEach(arr::add)
    return arr
}