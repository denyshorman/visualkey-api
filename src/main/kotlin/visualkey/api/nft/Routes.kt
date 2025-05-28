package visualkey.api.nft

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject
import visualkey.service.nft.NftService
import visualkey.util.serverUrl

fun Route.nftRoutes() {
    val nftService by inject<NftService>()

    get("/v1/nft") {
        val collectionMetadata = nftService.getCollectionMetadata()
        call.respond(collectionMetadata)
    }

    get("/v1/nft/tokens/{id}") {
        val serverUrl = call.request.origin.serverUrl()
        val tokenId = call.parameters.getOrFail("id").toTokenId()
        val level = call.request.queryParameters.getOrFail("level").toLevel()
        val power = call.request.queryParameters.getOrFail("power").toPower()
        val createdAt = call.request.queryParameters.getOrFail("createdAt").toEpochSeconds()
        val metadata = nftService.getTokenMetadata(serverUrl, tokenId, level, power, createdAt)
        call.respond(metadata)
    }

    get("/v1/nft/images/{id}.svg") {
        val token = call.parameters.getOrFail("id").toTokenId()
        val bitSize = call.request.queryParameters["size"]?.toBitSize() ?: DEFAULT_BIT_SIZE

        val image = nftService.generateSvgImage(
            token,
            bitSize,
            DEFAULT_FALSE_BIT_COLOR,
            DEFAULT_TRUE_BIT_COLOR,
        )

        call.response.header(HttpHeaders.CacheControl, CACHE_CONTROL_IMMUTABLE_YEAR)
        call.respondText(image, ContentType.Image.SVG, HttpStatusCode.OK)
    }

    listOf("/v1/nft/images/{id}", "/v1/nft/images/{id}.png").forEach { path ->
        get(path) {
            val token = call.parameters.getOrFail("id").toTokenId()
            val bitSize = call.request.queryParameters["size"]?.toBitSize() ?: DEFAULT_BIT_SIZE

            val image = nftService.generatePngImage(
                token,
                bitSize,
                DEFAULT_FALSE_BIT_COLOR,
                DEFAULT_TRUE_BIT_COLOR,
            )

            call.response.header(HttpHeaders.CacheControl, CACHE_CONTROL_IMMUTABLE_YEAR)
            call.respondBytes(image, ContentType.Image.PNG, HttpStatusCode.OK)
        }
    }
}

private const val DEFAULT_BIT_SIZE = 29u
private const val DEFAULT_FALSE_BIT_COLOR = "#ff0040"
private const val DEFAULT_TRUE_BIT_COLOR = "#30ff12"
private const val CACHE_CONTROL_IMMUTABLE_YEAR = "public, max-age=31536000, immutable"
