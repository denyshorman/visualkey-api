package visualkey.service.nft.signature.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.*
import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger
import visualkey.service.nft.IssuedSignature
import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PersistentMintSignatureRepository(
    private val uri: String,
    key: String,
    private val httpClient: HttpClient,
    private val clock: Clock,
) : MintSignatureRepository {
    private val signer = HmacSha256Signer(key)

    override suspend fun find(region: ChainRegion, token: BigInteger): IssuedSignature? {
        val id = region.serialize() + token.toString(16)
        val resourceLink = "dbs/visualkey/colls/mint-authorization/docs/$id"
        val date = clock.now().toRfc1123Date()
        val authorization = "get\ndocs\n$resourceLink\n${date.lowercase()}\n\n".toAuthorization()

        val resp = httpClient.get("$uri/$resourceLink") {
            header("x-ms-date", date)
            header("x-ms-version", API_VERSION)
            header("x-ms-documentdb-partitionkey", "[\"$id\"]")
            header(HttpHeaders.Authorization, authorization)
        }

        if (resp.status == HttpStatusCode.NotFound) {
            return null
        }

        if (resp.status != HttpStatusCode.OK) {
            val httpBody = resp.body<String>()
            throw Exception("Failed to get mint signature: $httpBody")
        }

        val result = resp.body<JsonObject>()

        val data = result["data"]?.jsonPrimitive?.contentOrNull ?: throw Exception("No data in the response")

        return data.deserializeSignature(token)
    }

    override suspend fun save(region: ChainRegion, sig: IssuedSignature) {
        val now = clock.now()
        val ttl = sig.mintDeadline.toLong() - now.epochSeconds + 180

        if (ttl < 0) {
            return
        }

        val resourceLink = "dbs/visualkey/colls/mint-authorization"
        val date = now.toRfc1123Date()
        val authorization = "post\ndocs\n$resourceLink\n${date.lowercase()}\n\n".toAuthorization()

        val id = region.serialize() + sig.token.toString(16)

        val body = buildJsonObject {
            put("id", id)
            put("data", sig.serialize())
            put("ttl", ttl)
        }

        val resp = httpClient.post("$uri/$resourceLink/docs") {
            header("x-ms-date", date)
            header("x-ms-version", API_VERSION)
            header("x-ms-documentdb-is-upsert", "true")
            header("x-ms-documentdb-partitionkey", "[\"$id\"]")
            header(HttpHeaders.Authorization, authorization)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (resp.status != HttpStatusCode.OK && resp.status != HttpStatusCode.Created) {
            val httpBody = resp.body<String>()
            throw Exception("Failed to save mint signature: $httpBody")
        }
    }

    private fun String.toAuthorization(): String {
        val signature = signer.sign(this)
        return URLEncoder.encode("type=master&ver=1.0&sig=$signature", Charsets.UTF_8)
    }

    private fun Instant.toRfc1123Date(): String {
        return RFC_1123_DATE_TIME.withZone(ZONE_GMT).format(toJavaInstant())
    }

    private fun IssuedSignature.serialize(): String {
        val chainIdStr = chainId.toString(16)
        val contractStr = contract.removePrefix("0x")
        val receiverStr = receiver.removePrefix("0x")
        val mintDeadlineStr = mintDeadline.toString(16)
        val mintSignatureStr = mintSignature.removePrefix("0x")
        return "$chainIdStr|$contractStr|$receiverStr|$mintDeadlineStr|$mintSignatureStr"
    }

    private fun String.deserializeSignature(token: BigInteger): IssuedSignature {
        split("|").let {
            return IssuedSignature(
                chainId = it[0].toULong(16),
                contract = "0x${it[1]}",
                receiver = "0x${it[2]}",
                token = token,
                mintDeadline = it[3].toULong(16),
                mintSignature = "0x${it[4]}",
            )
        }
    }

    private fun ChainRegion.serialize(): String {
        return when (this) {
            ChainRegion.Mainnet -> "0"
            ChainRegion.Testnet -> "1"
            ChainRegion.Local -> "2"
        }
    }

    private class HmacSha256Signer(key: String) {
        private val signingKey: SecretKeySpec
        private val macInstance: Mac

        init {
            val algorithm = "HmacSHA256"
            signingKey = SecretKeySpec(Base64.getDecoder().decode(key), algorithm)
            macInstance = Mac.getInstance(algorithm)
            macInstance.init(signingKey)
        }

        fun sign(msg: String): String {
            val mac = macInstance.clone() as Mac
            val sign = mac.doFinal(msg.toByteArray())
            return Base64.getEncoder().encodeToString(sign)
        }
    }

    companion object {
        private const val API_VERSION = "2020-07-15"
        private val ZONE_GMT = ZoneId.of("GMT")
        private val RFC_1123_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    }
}
