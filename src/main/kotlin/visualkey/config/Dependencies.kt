package visualkey.config

import org.koin.core.KoinApplication
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module
import visualkey.model.Environment
import visualkey.model.toEnvironment
import visualkey.service.nft.NftService

fun KoinApplication.loadDependencies() {
    val dependencies = module {
        single<NftService> {
            NftService(get(named("VISUAL_KEY_URL")))
        } withOptions {
            createdAtStart()
        }

        single<Environment> {
            System.getenv("ENVIRONMENT")?.toEnvironment() ?: Environment.Local
        } withOptions {
            createdAtStart()
        }

        single<String>(named("VISUAL_KEY_URL")) {
            System.getenv("VISUAL_KEY_URL") ?: "http://localhost:4200"
        } withOptions {
            createdAtStart()
        }
    }

    modules(dependencies)
}
