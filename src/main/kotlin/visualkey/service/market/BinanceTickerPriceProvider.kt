package visualkey.service.market

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import visualkey.serializer.BigDecimal

/**
 * Documentation https://binance-docs.github.io/apidocs/spot/en/
 */
class BinanceTickerPriceProvider(
    private val httpClient: HttpClient,
) : TickerPriceProvider {
    private val apiUrl = "https://api.binance.com"

    override suspend fun getPrice(currency: String): BigDecimal {
        val resp = httpClient.get("$apiUrl/api/v3/ticker/price") {
            parameter("symbol", "${currency}USDT")
        }

        val price = resp.body<Price>()

        return price.price
    }

    @Serializable
    private data class Price(
        val symbol: String,
        val price: BigDecimal,
    )
}