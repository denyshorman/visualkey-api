package visualkey.api.nft

import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun RateLimitConfig.nftRateLimits() {
    register(RateLimitName("/v1/nft/tokens/{id}/price")) {
        rateLimiter(limit = 1, refillPeriod = 1.seconds)
        requestKey { it.request.origin.remoteHost }
    }

    register(RateLimitName("/v1/nft/tokens/{id}/minting/authorization")) {
        rateLimiter(limit = 30, refillPeriod = 30.minutes)
        requestKey { it.request.origin.remoteHost }
    }
}
