package visualkey.api.common

import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import visualkey.model.Environment

fun Route.generalRoutes() {
    val env by inject<Environment>()

    rateLimit(RateLimitName("/v1/ping")) {
        get("/v1/ping") {
            call.respondText("pong", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }

    if (env == Environment.Local) {
        swaggerUI(path = "swagger", swaggerFile = "openapi.yml")
    }
}
