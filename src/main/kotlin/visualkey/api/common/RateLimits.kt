package visualkey.api.common

import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

fun RateLimitConfig.generalRateLimits() {
    register(RateLimitName("/v1/ping")) {
        rateLimiter(limit = 5, refillPeriod = 10.minutes)
        requestKey { it.request.origin.remoteHost }
    }
}
