package visualkey

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import visualkey.api.common.apiExceptionHandler
import visualkey.api.common.generalRateLimits
import visualkey.api.common.generalRoutes
import visualkey.api.nft.nftRoutes
import visualkey.config.loadDependencies

fun main() {
    embeddedServer(CIO, configure = {
        connectors.add(EngineConnectorBuilder().apply {
            host = System.getenv("HTTP_HOST") ?: "0.0.0.0"
            port = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080
        })

        shutdownGracePeriod = 2000
        shutdownTimeout = 60000
    }) {
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
            })
        }

        install(CORS) {
            anyHost()
            anyMethod()
            allowHeaders { true }
            allowNonSimpleContentTypes = true
        }

        install(Koin) {
            slf4jLogger()
            loadDependencies()
        }

        install(StatusPages) {
            apiExceptionHandler()
        }

        install(RateLimit) {
            generalRateLimits()
        }

        run {
            val behindProxy = System.getenv("BEHIND_PROXY")?.toBoolean() ?: false

            if (behindProxy) {
                install(XForwardedHeaders)
            }
        }

        routing {
            generalRoutes()
            nftRoutes()
        }
    }.start(true)
}
