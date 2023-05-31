package visualkey

import visualkey.api.common.apiExceptionHandler
import visualkey.api.common.generalRateLimits
import visualkey.api.common.generalRoutes
import visualkey.api.nft.nftRateLimits
import visualkey.api.nft.nftRoutes
import visualkey.config.loadDependencies
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    EngineMain.main(augment(args))
}

@Suppress("unused")
fun Application.main() {
    install(ContentNegotiation) {
        json(Json {
            explicitNulls = false
        })
    }

    install(CORS) {
        anyHost()
    }

    install(Koin) {
        slf4jLogger()
        loadDependencies(environment)
    }

    install(StatusPages) {
        apiExceptionHandler(this@main.environment)
    }

    install(RateLimit) {
        generalRateLimits()
        nftRateLimits()
    }

    run {
        val behindProxy = environment.config.property("app.behindProxy").getString().toBoolean()

        if (behindProxy) {
            install(XForwardedHeaders)
        }
    }

    routing {
        generalRoutes()
        nftRoutes()
    }
}

fun augment(args: Array<String>): Array<String> {
    val secretsFilePath = System.getenv("SECRETS_FILE_PATH") ?: return args

    val secretsFile = File(secretsFilePath)

    if (!secretsFile.exists()) {
        val secretsFileDataEncoded = System.getenv("SECRETS_FILE_DATA")
            ?: throw Exception("SECRETS_FILE_DATA environment variable is not set")

        val secretsFileData = Base64.getDecoder().decode(secretsFileDataEncoded)

        secretsFile.writeBytes(secretsFileData)
    }

    return args + arrayOf("-config=$secretsFilePath")
}
