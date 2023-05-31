package visualkey.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger as JBigInteger

object BigIntegerAsStringSerializer : KSerializer<JBigInteger> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): JBigInteger {
        return JBigInteger(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: JBigInteger) {
        encoder.encodeString(value.toString())
    }
}

typealias BigInteger = @Serializable(BigIntegerAsStringSerializer::class) JBigInteger
