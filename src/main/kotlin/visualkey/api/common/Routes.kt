package visualkey.api.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.generalRoutes() {
    rateLimit(RateLimitName("/v1/ping")) {
        get("/v1/ping") {
            call.respondText("pong", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}
