package utils

import io.ktor.http.HttpStatusCode


class StatusCodeException(val code: HttpStatusCode) : RuntimeException()