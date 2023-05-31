package visualkey.util

import visualkey.serializer.BigInteger

fun String.addHexPrefix(): String {
    return "0x$this"
}

fun String.containsHexPrefix(): Boolean {
    return length > 1 && this[0] == '0' && this[1] == 'x'
}

fun String.containsBinaryPrefix(): Boolean {
    return length > 1 && this[0] == '0' && this[1] == 'b'
}

fun String.toBigIntegerFromBinaryDecimalOrHex(): BigInteger {
    return if (containsHexPrefix()) {
        BigInteger(substring(2), 16)
    } else if (containsBinaryPrefix()) {
        BigInteger(substring(2), 2)
    } else {
        BigInteger(this)
    }
}

fun String.isHexDecimalNumber(): Boolean {
    if (length < 3) {
        return false
    }

    if (this[0] != '0' || this[1] != 'x') {
        return false
    }

    var i = 2

    while (i < length) {
        val valid = when (this[i]) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F' -> true
            else -> false
        }

        if (!valid) {
            return false
        }

        i++
    }

    return true
}
