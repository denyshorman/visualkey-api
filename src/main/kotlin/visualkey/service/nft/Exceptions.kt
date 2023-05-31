package visualkey.service.nft

import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger

data class InvalidSignatureException(
    val actual: String,
    val expected: String,
) : Exception("Invalid signature: actual=$actual, expected=$expected", null, true, false)

data class PriceExpiredException(
    val expirationTime: ULong,
    val serverTime: ULong,
) : Exception("Price expired: expirationTime=$expirationTime, serverTime=$serverTime", null, true, false)

data class SignerNotFoundException(
    val chainId: ULong,
    val contractAddress: String,
) : Exception("Signer not found for the chain $chainId and contract $contractAddress", null, true, false)

data class ChainNotSupportedException(
    val chainId: ULong,
) : Exception("The chain $chainId is not supported", null, true, false)

data class TokenAlreadyMintedException(
    val region: ChainRegion,
    val owner: String,
    val token: BigInteger,
) : Exception("The token $token is already minted by $owner in $region region", null, true, false)

data class PendingMintingException(
    val chainId: ULong,
    val contract: String,
    val receiver: String,
    val token: BigInteger,
    val mintDeadline: ULong,
) : Exception("Another user has requested minting authorization for the token $token", null, true, false)

data class TokenLockedException(
    val region: ChainRegion,
    val chainId: ULong,
    val token: BigInteger,
    val lockedBy: String,
    val lockedWhen: ULong,
) : Exception(
    "The token $token was locked in the $region region and $chainId chain by $lockedBy at $lockedWhen",
    null,
    true,
    false,
)

object PriceUnavailableException : Exception("The price cannot be determined at this time", null, true, false)
