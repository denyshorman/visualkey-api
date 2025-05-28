package visualkey.api.common

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import visualkey.model.Environment
import visualkey.model.toEnvironment

fun StatusPagesConfig.apiExceptionHandler() {
    val deployEnv = System.getenv("ENVIRONMENT")?.toEnvironment() ?: Environment.Local

    exception<MissingRequestParameterException> { call, cause ->
        val error = ErrorResponse(
            code = "BAD_REQUEST",
            message = "${cause.parameterName} parameter is missing",
        )

        call.respond(HttpStatusCode.BadRequest, error)
    }

    exception<BadRequestException> { call, cause ->
        val error = ErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Bad request")
        call.respond(HttpStatusCode.BadRequest, error)
    }

    status(HttpStatusCode.TooManyRequests) { call, status ->
        val retryAfter = call.response.headers["Retry-After"] ?: "60"

        val error = ErrorResponse(
            code = "TOO_MANY_REQUESTS",
            message = "Too many requests. Wait for $retryAfter seconds.",
            params = buildJsonObject {
                put("retryAfter", retryAfter)
            },
        )

        call.respond(status, error)
    }

    run {
        if (deployEnv == Environment.Local) {
            exception<Throwable> { call, cause ->
                val error = ErrorResponse(code = "INTERNAL_ERROR", message = cause.message ?: "Unknown error")
                call.respond(HttpStatusCode.InternalServerError, error)
                cause.printStackTrace()
            }
        } else {
            exception<Throwable> { call, cause ->
                val error = ErrorResponse(code = "INTERNAL_ERROR", message = "Internal error occurred")
                call.respond(HttpStatusCode.InternalServerError, error)
                cause.printStackTrace()
            }
        }
    }
}
