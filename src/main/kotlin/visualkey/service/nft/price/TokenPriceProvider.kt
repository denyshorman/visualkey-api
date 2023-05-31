package visualkey.service.nft.price

import visualkey.model.ChainRegion
import visualkey.serializer.BigDecimal
import visualkey.serializer.BigInteger
import visualkey.service.nft.ChainNotSupportedException
import visualkey.service.nft.resolver.ChainRegionResolver

class TokenPriceProvider(
    private val chainRegionResolver: ChainRegionResolver,
    private val priceUsdMainnet: BigDecimal,
    private val priceUsdTestnet: BigDecimal,
) {
    @Suppress("UNUSED_PARAMETER")
    @Throws(ChainNotSupportedException::class)
    fun getPrice(chainId: ULong, token: BigInteger): BigDecimal {
        val chainRegion = chainRegionResolver.resolve(chainId)
            ?: throw ChainNotSupportedException(chainId)

        return when (chainRegion) {
            ChainRegion.Mainnet -> priceUsdMainnet
            ChainRegion.Testnet -> priceUsdTestnet
            ChainRegion.Local -> priceUsdMainnet
        }
    }
}
