package visualkey.service.nft.signature.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger
import visualkey.service.nft.IssuedSignature

class InMemoryMintSignatureRepository(
    private val clock: Clock = Clock.System,
) : MintSignatureRepository {
    private val signatures = hashMapOf<RegionToken, IssuedSignature>()
    private val mutex = Mutex()

    override suspend fun find(region: ChainRegion, token: BigInteger): IssuedSignature? {
        mutex.withLock {
            return signatures[RegionToken(region, token)]
        }
    }

    override suspend fun save(region: ChainRegion, sig: IssuedSignature) {
        mutex.withLock {
            removeExpiredSignatures()
            signatures[RegionToken(region, sig.token)] = sig
        }
    }

    private fun removeExpiredSignatures() {
        val now = clock.now().epochSeconds.toULong()
        signatures.values.removeIf { it.mintDeadline < now }
    }

    private data class RegionToken(val region: ChainRegion, val token: BigInteger)
}
