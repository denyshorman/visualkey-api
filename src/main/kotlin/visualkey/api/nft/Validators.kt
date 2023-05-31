package visualkey.api.nft

import io.ktor.server.plugins.*
import visualkey.serializer.BigInteger
import visualkey.util.isHexDecimalNumber
import visualkey.util.toBigIntegerFromBinaryDecimalOrHex
import org.web3j.utils.EnsUtils.EMPTY_ADDRESS as ZERO_ADDRESS
import visualkey.api.nft.VisualKeyToken.isValid as isVisualKeyTokenValid

fun String.toVisualKeyToken(): BigInteger {
    val number = try {
        toBigIntegerFromBinaryDecimalOrHex()
    } catch (e: NumberFormatException) {
        throw BadRequestException("token must be a binary, decimal, or a hexadecimal number")
    }

    if (!number.isVisualKeyTokenValid()) {
        throw BadRequestException(
            "Token must be a number between " +
                    "${VisualKeyToken.MinValue.toString(10)} and " +
                    "0x${VisualKeyToken.MaxValue.toString(16)}"
        )
    }

    return number
}

fun String.toEvmAddress(): String {
    if (length != 42 || !isHexDecimalNumber()) {
        throw BadRequestException("Address must be a 42 character hexadecimal string")
    }

    return this
}

fun String.nonZero(param: String): String {
    if (this == ZERO_ADDRESS) {
        throw BadRequestException("$param must be a non-zero address")
    }

    return this
}

fun String.toChainId(): ULong {
    try {
        return toULong()
    } catch (e: NumberFormatException) {
        throw BadRequestException("chainId must be a positive integer")
    }
}

fun String.toPrice(): BigInteger {
    try {
        return BigInteger(this)
    } catch (e: NumberFormatException) {
        throw BadRequestException("price must be a positive integer")
    }
}

fun String.toPriceExpirationTime(): ULong {
    try {
        return toULong()
    } catch (e: NumberFormatException) {
        throw BadRequestException("priceExpirationTime must be a positive integer")
    }
}

private object VisualKeyToken {
    val MinValue: BigInteger = BigInteger.ONE
    val MaxValue: BigInteger = BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140", 16)

    fun BigInteger.isValid(): Boolean {
        return this in MinValue..MaxValue
    }
}
