package visualkey.api.nft

import io.ktor.server.plugins.*
import visualkey.util.toBigIntegerFromBinaryDecimalOrHex
import java.math.BigInteger

private val MinTokenIdValue = BigInteger.ONE
private val MaxTokenIdValue = BigInteger("ffffffffffffffffffffffffffffffffffffffff", 16)
private val MaxPowerValue = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)

private fun BigInteger.isTokenIdValid(): Boolean {
    return this in MinTokenIdValue..MaxTokenIdValue
}

fun String.toTokenId(): BigInteger {
    val number = try {
        toBigIntegerFromBinaryDecimalOrHex()
    } catch (_: NumberFormatException) {
        throw BadRequestException("token must be a binary, decimal, or a hexadecimal number")
    }

    if (!number.isTokenIdValid()) {
        throw BadRequestException(
            "Token must be a number between ${MinTokenIdValue.toString(10)} and 0x${MaxTokenIdValue.toString(16)}"
        )
    }

    return number
}

fun String.toLevel(): UByte {
    val level = try {
        toUByte()
    } catch (_: NumberFormatException) {
        throw BadRequestException("level must be a number between 0 and 160")
    }

    if (level < 0u || level > 160u) {
        throw BadRequestException("level must be a number between 0 and 160")
    }

    return level
}

fun String.toPower(): BigInteger {
    val power = try {
        toBigIntegerFromBinaryDecimalOrHex()
    } catch (_: NumberFormatException) {
        throw BadRequestException("power must be a binary, decimal, or a hexadecimal number")
    }

    if (power < BigInteger.ZERO || power > MaxPowerValue) {
        throw BadRequestException("power must be a positive 256 bit number")
    }

    return power
}

fun String.toEpochSeconds(): ULong {
    val epochSeconds = try {
        toLong()
    } catch (_: NumberFormatException) {
        throw BadRequestException("minted must be a valid Unix epoch timestamp in seconds")
    }

    if (epochSeconds < 0) {
        throw BadRequestException("minted must be a valid Unix epoch timestamp in seconds")
    }

    return epochSeconds.toULong()
}

fun String.toBitSize(): UInt {
    val bitSize = try {
        toUInt()
    } catch (_: NumberFormatException) {
        throw BadRequestException("size must be a positive integer")
    }

    if (bitSize < 1u || bitSize > 100u) {
        throw BadRequestException("size must be a positive integer between 1 and 100")
    }

    return bitSize
}
