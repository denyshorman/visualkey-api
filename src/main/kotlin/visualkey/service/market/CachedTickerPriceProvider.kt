package visualkey.service.market

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import visualkey.serializer.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class CachedTickerPriceProvider(
    private val provider: TickerPriceProvider,
    private val clock: Clock = Clock.System,
    private val ttl: Duration = 30.minutes,
) : TickerPriceProvider {
    private val cache = hashMapOf<String, PriceExpiry>()
    private val mutex = Mutex()

    override suspend fun getPrice(currency: String): BigDecimal {
        mutex.withLock {
            val priceExpiry = cache[currency]

            if (priceExpiry != null && priceExpiry.expiry > clock.now()) {
                return priceExpiry.price
            }

            val price = provider.getPrice(currency)

            cache[currency] = PriceExpiry(price, clock.now().plus(ttl))

            return price
        }
    }

    private data class PriceExpiry(val price: BigDecimal, val expiry: Instant)
}
