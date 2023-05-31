package visualkey.service.nft.signature.repository

import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger
import visualkey.service.nft.IssuedSignature

interface MintSignatureRepository {
    suspend fun find(region: ChainRegion, token: BigInteger): IssuedSignature?
    suspend fun save(region: ChainRegion, sig: IssuedSignature)
}
