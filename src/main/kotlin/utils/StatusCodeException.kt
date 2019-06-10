package utils

import io.ktor.http.HttpStatusCode
import java.lang.RuntimeException


class StatusCodeException(val code: HttpStatusCode) : RuntimeException()