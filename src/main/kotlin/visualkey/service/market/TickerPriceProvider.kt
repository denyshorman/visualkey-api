package visualkey.service.market

import visualkey.serializer.BigDecimal

interface TickerPriceProvider {
    suspend fun getPrice(currency: String): BigDecimal
}
