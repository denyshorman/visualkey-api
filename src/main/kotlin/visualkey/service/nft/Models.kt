package visualkey.service.nft

import visualkey.serializer.BigInteger
import kotlinx.serialization.Serializable

@Serializable
data class TokenPrice(
    val price: BigInteger,
    val expirationTime: Long,
    val signature: String,
)

@Serializable
data class MintAuthorization(
    val price: BigInteger? = null,
    val deadline: ULong,
    val signature: String,
)

@Serializable
data class Metadata(
    val name: String,
    val description: String,
    val image: String,
)

@Serializable
data class IssuedSignature(
    val chainId: ULong,
    val contract: String,
    val receiver: String,
    val token: BigInteger,
    val mintDeadline: ULong,
    val mintSignature: String,
)
