package visualkey.service.nft

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import visualkey.model.ChainRegion
import visualkey.serializer.BigInteger
import visualkey.service.contract.visualkey.VisualKeyContract
import visualkey.service.nft.price.TokenPriceCalculator
import visualkey.service.nft.resolver.ChainRegionResolver
import visualkey.service.nft.signature.MintSigner
import visualkey.service.nft.signature.repository.MintSignatureRepository
import visualkey.service.signer.AppSigner
import visualkey.util.KeyMutex
import visualkey.util.addrNe
import visualkey.util.encodePacked
import kotlin.math.floor
import kotlin.time.Duration

class NftService(
    private val appSigner: AppSigner,
    private val mintSigner: MintSigner,
    private val tokenPriceCalculator: TokenPriceCalculator,
    private val mintSignatureRepository: MintSignatureRepository,
    private val visualKeyContract: VisualKeyContract,
    private val chainRegionResolver: ChainRegionResolver,
    private val priceExpiryDuration: Duration,
    private val mintingPeriod: Duration,
    private val clock: Clock,
) {
    private val mintAuthorizationMutex = KeyMutex<Pair<ChainRegion, BigInteger>>()

    @Throws(ChainNotSupportedException::class, PriceUnavailableException::class)
    suspend fun getTokenPrice(
        chainId: ULong,
        token: BigInteger,
        receiver: String,
        checkDiscount: Boolean,
    ): TokenPrice {
        val price = tokenPriceCalculator.calculatePrice(chainId, token, receiver, checkDiscount)
        val priceExpirationTime = clock.now().plus(priceExpiryDuration).epochSeconds
        val signature = priceSignature(chainId, receiver, token, price, priceExpirationTime)
        return TokenPrice(price, priceExpirationTime, signature)
    }

    @Throws(
        SignerNotFoundException::class,
        ChainNotSupportedException::class,
        PriceUnavailableException::class,
        PendingMintingException::class,
    )
    suspend fun authorizeMinting(
        chainId: ULong,
        contract: String,
        receiver: String,
        token: BigInteger,
        checkDiscount: Boolean,
    ): MintAuthorization {
        return authorizeMinting(chainId, contract, receiver, token, tokenPrice = null, checkDiscount)
    }

    @Throws(
        InvalidSignatureException::class,
        PriceExpiredException::class,
        SignerNotFoundException::class,
        PendingMintingException::class,
    )
    suspend fun authorizeMinting(
        chainId: ULong,
        contract: String,
        receiver: String,
        token: BigInteger,
        price: BigInteger,
        priceExpirationTime: ULong,
        priceSignature: String,
    ): MintAuthorization {
        //#region Verify price validity
        val now = clock.now()
        val nowSeconds = now.epochSeconds.toULong()

        if (nowSeconds > priceExpirationTime) {
            throw PriceExpiredException(priceExpirationTime, nowSeconds)
        }

        val priceSignatureExpected = priceSignature(chainId, receiver, token, price, priceExpirationTime.toLong())

        if (priceSignature != priceSignatureExpected) {
            throw InvalidSignatureException(priceSignature, priceSignatureExpected)
        }
        //#endregion

        return authorizeMinting(
            chainId,
            contract,
            receiver,
            token,
            price,
            checkDiscount = false,
        )
    }

    fun getMetaData(serverUrl: String, token: BigInteger): Metadata {
        return Metadata(
            name = "VisualKey NFT",
            description = "Graphical representation of the 256-bit number $token. Mint your own at https://visualkey.link",
            image = "$serverUrl/v1/nft/images/$token",
        )
    }

    fun generateImage(
        token: BigInteger,
        tokenSize: UInt,
        bgColor: String,
        falseBitColor: String,
        trueBitColor: String,
    ): String {
        val bitSize = tokenSize / TOKEN_COLS

        val svg = buildString(capacity = 8400) {
            append("""<svg width="$tokenSize" height="$tokenSize" viewBox="0 0 $tokenSize $tokenSize" style="position:absolute;margin:auto;inset:0;background:$bgColor" xmlns="http://www.w3.org/2000/svg">""")
            append("<defs>")
            append("""<rect id="b" width="$bitSize" height="$bitSize" stroke-width="1" stroke="#44464d" stroke-opacity=".9"/>""")
            append("""<use id="f" href="#b" fill="$falseBitColor"/>""")
            append("""<use id="t" href="#b" fill="$trueBitColor"/>""")
            append("</defs>")

            var i = 0u
            while (i < TOKEN_BIT_LENGTH) {
                val x = bitSize * (i % TOKEN_COLS)
                val y = bitSize * floor(i.toDouble() / TOKEN_COLS.toDouble()).toUInt()

                val bitValue = token.and(BigInteger.ONE.shl((TOKEN_BIT_LENGTH - i - 1u).toInt())) > BigInteger.ZERO
                val color = if (bitValue) "t" else "f"

                append("""<use href="#$color" x="$x" y="$y"/>""")

                i++
            }

            append("</svg>")
        }

        return svg
    }

    //#region Signature Generators and Checkers
    private fun priceSignature(
        chainId: ULong,
        receiver: String,
        token: BigInteger,
        price: BigInteger,
        priceExpirationTime: Long,
    ): String {
        val params = sequenceOf(
            Uint256(chainId.toLong()),
            Address(receiver),
            Uint256(token),
            Uint256(price),
            Uint256(priceExpirationTime),
        )

        return appSigner.sign(params.encodePacked())
    }

    @Throws(SignerNotFoundException::class)
    private fun mintSignature(
        chainId: ULong,
        contract: String,
        receiver: String,
        token: BigInteger,
        price: BigInteger,
        deadline: ULong
    ): String {
        val params = sequenceOf(
            Uint256(chainId.toLong()),
            Address(contract),
            Address(receiver),
            Uint256(token),
            Uint256(price),
            Uint256(deadline.toLong()),
        )

        return mintSigner.sign(chainId, contract, params.encodePacked())
    }

    private suspend fun authorizeMinting(
        chainId: ULong,
        contract: String,
        receiver: String,
        token: BigInteger,
        tokenPrice: BigInteger?,
        checkDiscount: Boolean,
    ): MintAuthorization {
        val chainRegion = chainRegionResolver.resolve(chainId)
            ?: throw ChainNotSupportedException(chainId)

        return mintAuthorizationMutex.withLock(chainRegion to token) {
            coroutineScope {
                launch {
                    val issuedSig = mintSignatureRepository.find(chainRegion, token)

                    if (issuedSig != null) {
                        if (
                            (issuedSig.chainId != chainId || issuedSig.contract addrNe contract) &&
                            (clock.now().epochSeconds.toULong() <= issuedSig.mintDeadline)
                        ) {
                            throw PendingMintingException(
                                issuedSig.chainId,
                                issuedSig.contract,
                                issuedSig.receiver,
                                issuedSig.token,
                                issuedSig.mintDeadline,
                            )
                        }
                    }
                }

                launch {
                    val owner = visualKeyContract.ownerOf(chainRegion, token)

                    if (owner != null) {
                        throw TokenAlreadyMintedException(chainRegion, owner, token)
                    }

                    val latestLock = visualKeyContract.getLocks(chainRegion, token)
                        .toList()
                        .maxByOrNull { it.lockedWhen }

                    if (latestLock != null && latestLock.lockedBy addrNe receiver) {
                        throw TokenLockedException(
                            chainRegion,
                            latestLock.chainId,
                            token,
                            latestLock.lockedBy,
                            latestLock.lockedWhen,
                        )
                    }
                }
            }

            val price = tokenPrice ?: tokenPriceCalculator.calculatePrice(chainId, token, receiver, checkDiscount)
            val mintDeadline = clock.now().plus(mintingPeriod).epochSeconds.toULong()
            val mintSignature = mintSignature(chainId, contract, receiver, token, price, mintDeadline)

            mintSignatureRepository.save(
                chainRegion,
                IssuedSignature(chainId, contract, receiver, token, mintDeadline, mintSignature),
            )

            MintAuthorization(price, mintDeadline, mintSignature)
        }
    }
    //#endregion

    companion object {
        private val TOKEN_COLS = 16u
        private val TOKEN_BIT_LENGTH = 256u
    }
}
