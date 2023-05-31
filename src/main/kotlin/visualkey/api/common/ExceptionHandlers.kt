package visualkey.api.common

import visualkey.model.Environment
import visualkey.model.toEnvironment
import visualkey.service.nft.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun StatusPagesConfig.apiExceptionHandler(environment: ApplicationEnvironment) {
    val deployEnv = environment.config.property("app.environment").getString().toEnvironment()

    exception<InvalidSignatureException> { call, cause ->
        val error = ErrorResponse(
            code = "INVALID_SIGNATURE",
            message = cause.message ?: "Invalid signature",
            params = buildJsonObject {
                put("actual", cause.actual)
                put("expected", cause.expected)
            },
        )
        call.respond(HttpStatusCode.BadRequest, error)
    }

    exception<PriceExpiredException> { call, cause ->
        val error = ErrorResponse(
            code = "PRICE_EXPIRED",
            message = cause.message ?: "Invalid signature",
            params = buildJsonObject {
                put("expirationTime", cause.expirationTime.toLong())
                put("serverTime", cause.serverTime.toLong())
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

    exception<SignerNotFoundException> { call, cause ->
        val error = ErrorResponse(
            code = "SIGNER_NOT_FOUND",
            message = cause.message ?: "Signer not found",
            params = buildJsonObject {
                put("chainId", cause.chainId.toLong())
                put("contractAddress", cause.contractAddress)
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

    exception<ChainNotSupportedException> { call, cause ->
        val error = ErrorResponse(
            code = "CHAIN_NOT_SUPPORTED",
            message = cause.message ?: "Chain not supported",
            params = buildJsonObject {
                put("chainId", cause.chainId.toLong())
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

    exception<TokenAlreadyMintedException> { call, cause ->
        val error = ErrorResponse(
            code = "TOKEN_ALREADY_MINTED",
            message = cause.message ?: "Token already minted",
            params = buildJsonObject {
                put("region", cause.region.name.lowercase())
                put("owner", cause.owner)
                put("token", cause.token.toString())
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

    exception<PendingMintingException> { call, cause ->
        val error = ErrorResponse(
            code = "PENDING_MINTING",
            message = cause.message ?: "Pending minting",
            params = buildJsonObject {
                put("chainId", cause.chainId.toLong())
                put("contract", cause.contract)
                put("receiver", cause.receiver)
                put("token", cause.token.toString())
                put("mintDeadline", cause.mintDeadline.toLong())
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

    exception<TokenLockedException> { call, cause ->
        val error = ErrorResponse(
            code = "TOKEN_LOCKED",
            message = cause.message ?: "Token is locked",
            params = buildJsonObject {
                put("region", cause.region.name.lowercase())
                put("chainId", cause.chainId.toLong())
                put("token", cause.token.toString())
                put("lockedBy", cause.lockedBy)
                put("lockedWhen", cause.lockedWhen.toLong())
            },
        )
        call.respond(HttpStatusCode.UnprocessableEntity, error)
    }

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
