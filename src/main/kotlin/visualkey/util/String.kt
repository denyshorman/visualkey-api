package visualkey.util

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayOutputStream
import java.math.BigInteger

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

fun String.toPng(): ByteArray {
    return byteInputStream(Charsets.UTF_8).use { inputStream ->
        ByteArrayOutputStream().use { outputStream ->
            PNGTranscoder().transcode(TranscoderInput(inputStream), TranscoderOutput(outputStream))
            outputStream.toByteArray()
        }
    }
}
