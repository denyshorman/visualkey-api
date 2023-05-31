package visualkey.service.contract.visualkey

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger
import java.util.concurrent.atomic.AtomicLong
import org.web3j.utils.EnsUtils.EMPTY_ADDRESS as ZERO_ADDRESS

class VisualKeyContract(
    private val chains: List<Chain>,
    private val httpClient: HttpClient,
) {
    //#region Public API
    @Throws(InvalidTokenException::class)
    suspend fun ownerOf(region: ChainRegion, token: BigInteger): String? {
        try {
            coroutineScope {
                for (chain in chains.asSequence().filter { it.region == region }) {
                    for (contract in chain.nftContracts) {
                        launch {
                            var latestException: Exception? = null

                            for (apiUrl in chain.apiUrls) {
                                try {
                                    val owner = ownerOf(apiUrl, contract, token)

                                    if (owner != null) throw OwnerExists(owner)

                                    return@launch
                                } catch (e: OwnerExists) {
                                    throw e
                                } catch (e: Exception) {
                                    latestException = e
                                }
                            }

                            throw latestException ?: RuntimeException("No API defined for the contract $contract")
                        }
                    }
                }
            }
        } catch (e: OwnerExists) {
            return e.owner
        }

        return null
    }

    fun getLocks(region: ChainRegion, token: BigInteger): Flow<Lock> {
        return channelFlow {
            for (chain in chains.asSequence().filter { it.region == region }) {
                for (contract in chain.nftContracts) {
                    launch {
                        var latestException: Exception? = null

                        for (apiUrl in chain.apiUrls) {
                            try {
                                val tokenLock = getLocks(apiUrl, contract, token)

                                if (tokenLock.lockedBy != ZERO_ADDRESS) {
                                    val lock = Lock(
                                        chain.id,
                                        contract,
                                        tokenLock.lockedBy,
                                        tokenLock.lockedWhen,
                                    )

                                    send(lock)
                                }

                                return@launch
                            } catch (e: Exception) {
                                latestException = e
                            }
                        }

                        throw latestException ?: RuntimeException("No API defined for the contract $contract")
                    }
                }
            }
        }.buffer(UNLIMITED)
    }
    //#endregion

    //#region Private API
    private suspend fun ownerOf(apiUrl: String, vkContractAddress: String, token: BigInteger): String? {
        val ownerOfFunc = Function("ownerOf", listOf(Uint256(token)), emptyList())
        val encodedFunction = FunctionEncoder.encode(ownerOfFunc)

        return try {
            val response = ethCall(apiUrl, vkContractAddress, encodedFunction)
            TypeDecoder.decodeAddress(response).value
        } catch (e: CustomError) {
            if (e.tokenDoesNotExistError()) {
                null
            } else if (e.tokenInvalidError()) {
                throw InvalidTokenException(token)
            } else {
                throw RuntimeException("Unknown error ownerOf($token, $vkContractAddress)", e)
            }
        }
    }

    private suspend fun getLocks(
        apiUrl: String,
        vkContractAddress: String,
        token: BigInteger,
    ): TokenLock {
        val getLockFunc = Function("getLock", listOf(Uint256(token)), emptyList())
        val encodedFunction = FunctionEncoder.encode(getLockFunc)
        val response = ethCall(apiUrl, vkContractAddress, encodedFunction)
        return TypeDecoder.decodeDynamicStruct(response, 0, TokenLock.TypeReference)
    }
    //#endregion

    //#region Ethereum API
    private suspend fun ethCall(apiUrl: String, vkContractAddress: String, data: String): String {
        val payload = buildJsonObject {
            put("id", idCounter.getAndIncrement())
            put("jsonrpc", "2.0")
            put("method", "eth_call")
            putJsonArray("params") {
                addJsonObject {
                    put("to", vkContractAddress)
                    put("data", data)
                }
                add("latest")
            }
        }

        val resp = httpClient.post(apiUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        val body = resp.body<JsonObject>()

        val error = body["error"]

        if (error != null) {
            when (val errorData = error.jsonObject["data"]) {
                is JsonPrimitive -> {
                    val errMsg = error.jsonObject["message"]!!.jsonPrimitive.content
                    val errData = errorData.content
                    throw CustomError(errMsg, errData)
                }

                is JsonObject -> {
                    val errMsg = errorData["message"]!!.jsonPrimitive.content
                    val errData = errorData["data"]!!.jsonPrimitive.content
                    throw CustomError(errMsg, errData)
                }

                else -> throw Exception("RPC error does not contain the data field")
            }
        }

        return body["result"]?.jsonPrimitive?.contentOrNull ?: throw Exception("No result in the response")
    }
    //#endregion

    //#region Public Models
    data class InvalidTokenException(val token: BigInteger) : Exception("Invalid token $token")

    data class Chain(
        val id: ULong,
        val region: ChainRegion,
        val apiUrls: List<String>,
        val nftContracts: List<String>,
    )

    data class Lock(
        val chainId: ULong,
        val contract: String,
        val lockedBy: String,
        val lockedWhen: ULong,
    )
    //#endregion

    //#region Private Models
    private data class TokenLock(
        val lockedBy: String,
        val lockedWhen: ULong,
    ) : DynamicStruct(
        Address(lockedBy),
        Uint256(lockedWhen.toLong()),
    ) {
        @Suppress("unused")
        constructor(
            lockedBy: Address,
            lockedWhen: Uint256,
        ) : this(
            lockedBy.value,
            lockedWhen.value.toLong().toULong(),
        )

        companion object {
            val TypeReference = object : TypeReference<TokenLock>() {}
        }
    }
    //#endregion

    //#region Private Exceptions
    private data class OwnerExists(val owner: String) : Exception("", null, true, false)
    private data class CustomError(val msg: String, val data: String) : Exception(msg, null, true, false)

    private fun CustomError.tokenDoesNotExistError(): Boolean {
        return this.data.startsWith("0xc927e5bf")
    }

    private fun CustomError.tokenInvalidError(): Boolean {
        return this.data.startsWith("0x814bac8e")
    }
    //#endregion

    companion object {
        private val idCounter = AtomicLong(0)
    }
}
