package visualkey.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.withOptions
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module
import visualkey.model.ChainRegion
import visualkey.model.Environment
import visualkey.model.toChainRegion
import visualkey.model.toEnvironment
import visualkey.service.contract.visualkey.VisualKeyContract
import visualkey.service.market.BinanceTickerPriceProvider
import visualkey.service.market.CachedTickerPriceProvider
import visualkey.service.market.TickerPriceProvider
import visualkey.service.nft.NftService
import visualkey.service.nft.price.TokenPriceCalculator
import visualkey.service.nft.price.TokenPriceProvider
import visualkey.service.nft.resolver.ChainCurrencyResolver
import visualkey.service.nft.resolver.ChainRegionResolver
import visualkey.service.nft.signature.MintSigner
import visualkey.service.nft.signature.repository.MintSignatureRepository
import visualkey.service.nft.signature.repository.PersistentMintSignatureRepository
import visualkey.service.signer.AppSigner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun KoinApplication.loadDependencies(environment: ApplicationEnvironment) {
    val dependencies = module {
        single<NftService> {
            val priceExpiryDuration = Duration.parse(environment.config.property("app.nft.priceExpiryDuration").getString())
            val mintingPeriod = Duration.parse(environment.config.property("app.nft.mintingPeriod").getString())
            NftService(get(), get(), get(), get(), get(), get(), priceExpiryDuration, mintingPeriod, get())
        } withOptions {
            createdAtStart()
        }

        single<TokenPriceProvider> {
            val priceUsdMainnet = environment.config.property("app.nft.priceUsdMainnet").getString().toBigDecimal()
            val priceUsdTestnet = environment.config.property("app.nft.priceUsdTestnet").getString().toBigDecimal()
            TokenPriceProvider(get(), priceUsdMainnet, priceUsdTestnet)
        } withOptions {
            createdAtStart()
        }

        single<TokenPriceCalculator> {
            TokenPriceCalculator(get(), get(), get(), get(), get())
        } withOptions {
            createdAtStart()
        }

        single<ChainRegionResolver> {
            val chainRegionMap = environment.config.configList("chains")
                .associate {
                    val chainId = it.property("chainId").getString().toULong()
                    val region = it.property("region").getString()
                    chainId to region
                }

            ChainRegionResolver(chainRegionMap)
        } withOptions {
            createdAtStart()
        }

        single<ChainCurrencyResolver> {
            val chainCurrencyMap = environment.config.configList("chains")
                .associate {
                    val chainId = it.property("chainId").getString().toULong()
                    val currency = it.property("currency").getString()
                    chainId to currency
                }

            ChainCurrencyResolver(chainCurrencyMap)
        } withOptions {
            createdAtStart()
        }

        single<TickerPriceProvider> {
            val binanceProvider = BinanceTickerPriceProvider(get())
            val cachedProvider = CachedTickerPriceProvider(binanceProvider, get(), ttl = 30.minutes)
            cachedProvider
        } withOptions {
            createdAtStart()
        }

        single<MintSignatureRepository> {
            val uri = environment.config.property("databases.visualKeyCosmosDb.uri").getString()
            val key = environment.config.property("databases.visualKeyCosmosDb.key").getString()
            PersistentMintSignatureRepository(uri, key, get(), get())
        } withOptions {
            createdAtStart()
        }

        single<MintSigner> {
            val chainPkMap = environment.config.configList("nfts")
                .associate {
                    val chainId = it.property("chainId").getString().toULong()
                    val addressPkMap = it.configList("contracts").associate { contract ->
                        val contractAddress = contract.property("address").getString()
                        val privateKey = contract.property("signerPrivateKey").getString()
                        contractAddress to privateKey
                    }
                    chainId to addressPkMap
                }

            MintSigner(chainPkMap)
        } withOptions {
            createdAtStart()
        }

        single<VisualKeyContract> {
            data class ChainIdApiUrls(
                val chainId: ULong,
                val apiUrls: List<String>,
                val region: ChainRegion,
            )

            data class ChainIdContracts(
                val chainId: ULong,
                val contracts: List<String>,
            )

            val apiUrls = environment.config.configList("chains").asSequence()
                .map { chain ->
                    val chainId = chain.property("chainId").getString().toULong()
                    val apiUrls = chain.property("apis").getList()
                    val chainRegion = chain.property("region").getString().toChainRegion()
                    ChainIdApiUrls(chainId, apiUrls, chainRegion)
                }
                .toList()

            val contracts = environment.config.configList("nfts").asSequence()
                .map { chain ->
                    val chainId = chain.property("chainId").getString().toULong()
                    val contracts = chain.configList("contracts").asSequence()
                        .map { it.property("address").getString() }
                        .toList()

                    ChainIdContracts(chainId, contracts)
                }

            val chains = contracts.map { (chainId, contracts) ->
                val chain = apiUrls.firstOrNull { it.chainId == chainId }
                    ?: throw RuntimeException("No API URL defined for the chain $chainId")

                VisualKeyContract.Chain(chain.chainId, chain.region, chain.apiUrls, contracts)
            }.toList()

            VisualKeyContract(chains, get())
        } withOptions {
            createdAtStart()
        }

        single<AppSigner> {
            val signerPrivateKey = environment.config.property("app.signerPrivateKey").getString()
            AppSigner(signerPrivateKey)
        } withOptions {
            createdAtStart()
        }

        single<Environment> {
            environment.config.property("app.environment").getString().toEnvironment()
        } withOptions {
            createdAtStart()
        }

        single<HttpClient> {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        explicitNulls = false
                    })
                }

                install(UserAgent) {
                    agent = "visualkey.link"
                }
            }

            registerCallback(object : ScopeCallback {
                override fun onScopeClose(scope: Scope) {
                    client.close()
                }
            })

            client
        } withOptions {
            createdAtStart()
        }

        single<Clock> { Clock.System } withOptions {
            createdAtStart()
        }
    }

    modules(dependencies)
}
