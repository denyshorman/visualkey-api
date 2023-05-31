package visualkey.service.nft.resolver

import visualkey.model.ChainRegion
import visualkey.model.toChainRegion

class ChainRegionResolver(chainRegionMap: Map<ULong, String>) {
    private val chainRegion = chainRegionMap.mapValues { it.value.toChainRegion() }

    fun resolve(chainId: ULong): ChainRegion? {
        return chainRegion[chainId]
    }
}
