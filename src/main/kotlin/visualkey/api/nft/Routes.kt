package visualkey.api.nft

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject
import visualkey.service.nft.MintAuthorization
import visualkey.service.nft.NftService
import visualkey.util.serverUrl

fun Route.nftRoutes() {
    val nftService by inject<NftService>()

    get("/v1/nft/tokens/{id}") {
        val serverUrl = call.request.origin.serverUrl()
        val token = call.parameters.getOrFail("id").toVisualKeyToken()
        val metadata = nftService.getMetaData(serverUrl, token)
        call.respond(metadata)
    }

    get("/v1/nft/images/{id}") {
        val token = call.parameters.getOrFail("id").toVisualKeyToken()
        val size = call.request.queryParameters["size"]?.toUIntOrNull() ?: 352u

        val image = nftService.generateImage(
            token,
            size,
            bgColor = "black",
            falseBitColor = "#ff0040",
            trueBitColor = "#30ff12",
        )

        call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
        call.respondText(image, ContentType.Image.SVG, HttpStatusCode.OK)
    }

    rateLimit(RateLimitName("/v1/nft/tokens/{id}/price")) {
        get("/v1/nft/tokens/{id}/price") {
            val token = call.parameters.getOrFail("id").toVisualKeyToken()
            val chainId = call.request.queryParameters.getOrFail("chainId").toChainId()
            val receiver = call.request.queryParameters.getOrFail("receiver").toEvmAddress().nonZero("receiver")
            val checkDiscount = call.request.queryParameters["checkDiscount"]?.toBooleanStrictOrNull() ?: false

            val price = nftService.getTokenPrice(chainId, token, receiver, checkDiscount)

            call.respond(price)
        }
    }

    rateLimit(RateLimitName("/v1/nft/tokens/{id}/minting/authorization")) {
        get("/v1/nft/tokens/{id}/minting/authorization") {
            val token = call.parameters.getOrFail("id").toVisualKeyToken()
            val chainId = call.request.queryParameters.getOrFail("chainId").toChainId()
            val contract = call.request.queryParameters.getOrFail("contract").toEvmAddress()
            val receiver = call.request.queryParameters.getOrFail("receiver").toEvmAddress().nonZero("receiver")
            val checkDiscount = call.request.queryParameters["checkDiscount"]?.toBooleanStrictOrNull() ?: false
            val price = call.request.queryParameters["price"]
            val priceExpirationTime = call.request.queryParameters["priceExpirationTime"]
            val priceSignature = call.request.queryParameters["priceSignature"]

            if (price != null && priceExpirationTime != null && priceSignature != null) {
                val authorization = nftService.authorizeMinting(
                    chainId,
                    contract,
                    receiver,
                    token,
                    price.toPrice(),
                    priceExpirationTime.toPriceExpirationTime(),
                    priceSignature,
                ).let { MintAuthorization(deadline = it.deadline, signature = it.signature) }

                call.respond(authorization)
            } else {
                val authorization = nftService.authorizeMinting(chainId, contract, receiver, token, checkDiscount)
                call.respond(authorization)
            }
        }
    }
}
