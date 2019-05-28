package utils

import com.google.gson.JsonParser
import io.ktor.http.HttpStatusCode

val jsonParser by lazy { JsonParser() }
val ImATeapot = HttpStatusCode(418, "I'm a teapot")