package visualkey.util

import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

fun String.sign(credentials: Credentials): String {
    val signature = Sign.signMessage(
        Numeric.hexStringToByteArray(this),
        credentials.ecKeyPair,
    )

    return Numeric.toHexString(
        byteArrayOf(
            *signature.r,
            *signature.s,
            *signature.v,
        )
    )
}

fun <T> Sequence<Type<out T>>.encodePacked(): String {
    return map { TypeEncoder.encodePacked(it) }.joinToString("")
}
