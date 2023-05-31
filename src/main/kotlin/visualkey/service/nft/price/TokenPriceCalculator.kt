package visualkey.service.nft.price

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.web3j.utils.Convert
import visualkey.serializer.BigInteger
import visualkey.service.contract.visualkey.VisualKeyContract
import visualkey.service.market.TickerPriceProvider
import visualkey.service.nft.ChainNotSupportedException
import visualkey.service.nft.PriceUnavailableException
import visualkey.service.nft.resolver.ChainCurrencyResolver
import visualkey.service.nft.resolver.ChainRegionResolver
import visualkey.util.addrEq
import java.math.RoundingMode

class TokenPriceCalculator(
    private val chainCurrencyResolver: ChainCurrencyResolver,
    private val chainRegionResolver: ChainRegionResolver,
    private val tokenPriceProvider: TokenPriceProvider,
    private val tickerPriceProvider: TickerPriceProvider,
    private val visualKeyContract: VisualKeyContract,
) {
    @Throws(ChainNotSupportedException::class, PriceUnavailableException::class)
    suspend fun calculatePrice(
        chainId: ULong,
        token: BigInteger,
        owner: String,
        checkDiscount: Boolean = false,
    ): BigInteger {
        if (checkDiscount) {
            val chainRegion = chainRegionResolver.resolve(chainId)
                ?: throw ChainNotSupportedException(chainId)

            val zeroPrice = coroutineScope {
                val noOwner = async { visualKeyContract.ownerOf(chainRegion, token) == null }

                val tokenLocked = async {
                    val lockedBy = visualKeyContract.getLocks(chainRegion, token)
                        .toList()
                        .maxByOrNull { it.lockedWhen }
                        ?.lockedBy

                    owner addrEq lockedBy
                }

                noOwner.await() && tokenLocked.await()
            }

            if (zeroPrice) {
                return BigInteger.ZERO
            }
        }

        val currency = chainCurrencyResolver.resolve(chainId) ?: throw ChainNotSupportedException(chainId)

        val currencyPrice = tickerPriceProvider.getPrice(currency)
        val tokenPrice = tokenPriceProvider.getPrice(chainId, token)

        try {
            val price = tokenPrice.divide(currencyPrice, 8, RoundingMode.HALF_EVEN)
            return Convert.toWei(price, Convert.Unit.ETHER).toBigInteger()
        } catch (e: ArithmeticException) {
            throw PriceUnavailableException
        }
    }
}
