package visualkey.service.nft.resolver

class ChainCurrencyResolver(private val chainCurrencyMap: Map<ULong, String>) {
    fun resolve(chainId: ULong): String? {
        return chainCurrencyMap[chainId]
    }
}
