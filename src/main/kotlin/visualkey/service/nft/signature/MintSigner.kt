package visualkey.service.nft.signature

import visualkey.service.nft.SignerNotFoundException
import visualkey.util.sign
import org.web3j.crypto.Credentials

class MintSigner(chainPkMap: Map<ULong, Map<String, String>>) {
    private val chainSignerMap = chainPkMap.mapValues { (_, contractPkMap) ->
        contractPkMap.mapValues { (_, pk) -> Credentials.create(pk)!! }
    }

    @Throws(SignerNotFoundException::class)
    fun sign(chainId: ULong, contractAddress: String, message: String): String {
        val credentials = chainSignerMap[chainId]?.get(contractAddress)
            ?: throw SignerNotFoundException(chainId, contractAddress)

        return message.sign(credentials)
    }
}
