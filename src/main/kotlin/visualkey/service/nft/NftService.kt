@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package visualkey.service.nft

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import org.web3j.utils.Convert
import visualkey.util.toPng
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class NftService(
    private val visualKeyUrl: String
) {
    fun getCollectionMetadata(): CollectionMetadata {
        return CollectionMetadata(
            name = "Visual Keys",
            symbol = "VKEYNFT",
            description = """Visual Keys is an NFT collection that transforms Ethereum addresses into visual patterns — grids of colored squares mapped from the binary structure of each address. Each NFT’s rarity is defined by three traits: Level (number of red squares at the start, representing leading zero bits), Power (how many VKEY tokens were burned to mint or enhance it), and Creation Date. Every NFT is fully on-chain and cryptographically bound to the address it represents.""",
            image = "$visualKeyUrl/assets/nft/350x350.png",
            bannerImage = "$visualKeyUrl/assets/nft/2800x1050.png",
            featuredImage = "$visualKeyUrl/assets/nft/600x400.png",
            externalLink = visualKeyUrl,
            collaborators = emptyList(),
        )
    }

    fun getTokenMetadata(
        serverUrl: String,
        tokenId: BigInteger,
        level: UByte,
        power: BigInteger,
        createdAt: ULong,
    ): TokenMetadata {
        val tokenIdHex = tokenId.toString(16).padStart(40, '0')
        val tokenIdHexPrefixed = "0x$tokenIdHex"
        val tokenName = "Visual Key #$tokenIdHex"
        val tokenClass = getTokenClass(level)
        val formattedPower = Convert.fromWei(power.toBigDecimal(), Convert.Unit.ETHER).toPlainString()
        val formattedCreationDate = formatTokenDescriptionDate(createdAt)

        return TokenMetadata(
            name = tokenName,
            description = "A Visual Key representing the Ethereum address $tokenIdHexPrefixed. Its Level is $level ($tokenClass). Power: $formattedPower VKEY. Created on $formattedCreationDate.",
            image = "$serverUrl/v1/nft/images/$tokenIdHexPrefixed",
            externalUrl = "$visualKeyUrl/nft/view/$tokenIdHexPrefixed",
            bgColor = "000000",
            attributes = listOf(
                TokenMetadata.Attribute("Level", JsonPrimitive(level)),
                TokenMetadata.Attribute("Power", JsonUnquotedLiteral(formattedPower)),
                TokenMetadata.Attribute("Class", JsonPrimitive(tokenClass)),
                TokenMetadata.Attribute("Creation Date", JsonPrimitive(createdAt), displayType = "date"),
            ),
        )
    }

    fun generateSvgImage(
        token: BigInteger,
        bitSize: UInt,
        falseBitColor: String,
        trueBitColor: String,
    ): String {
        val width = bitSize * TOKEN_COLS
        val height = bitSize * (TOKEN_BIT_LENGTH / TOKEN_COLS)

        val svg = buildString(capacity = 6300) {
            append("""<svg width="$width" height="$height" viewBox="0 0 $width $height" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">""")
            append("<defs>")
            append("""<rect id="b" width="$bitSize" height="$bitSize" stroke-width="1" stroke="#44464d" stroke-opacity=".9"/>""")
            append("""<use id="f" xlink:href="#b" fill="$falseBitColor"/>""")
            append("""<use id="t" xlink:href="#b" fill="$trueBitColor"/>""")
            append("</defs>")

            var i = 0u
            while (i < TOKEN_BIT_LENGTH) {
                val x = bitSize * (i % TOKEN_COLS)
                val y = bitSize * (i / TOKEN_COLS)

                val bit = token.and(BigInteger.ONE.shl((TOKEN_BIT_LENGTH - i - 1u).toInt())) > BigInteger.ZERO
                val color = if (bit) "t" else "f"

                append("""<use xlink:href="#$color" x="$x" y="$y"/>""")

                i++
            }

            append("</svg>")
        }

        return svg
    }

    fun generatePngImage(
        token: BigInteger,
        bitSize: UInt,
        falseBitColor: String,
        trueBitColor: String,
    ): ByteArray {
        return generateSvgImage(token, bitSize, falseBitColor, trueBitColor).toPng()
    }

    companion object {
        private val TOKEN_COLS = 16u
        private val TOKEN_BIT_LENGTH = 160u

        private val tokenCreationDateFormatter = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC)

        private fun getTokenClass(level: UByte): String {
            val leadingZeros = level.toInt() / 4

            return when (leadingZeros) {
                0 -> "Common"
                1 -> "Notable"
                2 -> "Rare"
                3 -> "Epic"
                4 -> "Heroic"
                5 -> "Fabled"
                6 -> "Legendary"
                7 -> "Mythic"
                8 -> "Ancient"
                9 -> "Exalted"
                10 -> "Illustrious"
                11 -> "Titan"
                12 -> "Primordial"
                13 -> "Divine"
                14 -> "Ascended"
                15 -> "Immortal"
                16 -> "Eternal"
                17 -> "Sovereign"
                18 -> "Paragon"
                19 -> "Unfathomable"
                20 -> "Demiurge"
                21 -> "Celestial"
                22 -> "Cosmic"
                23 -> "Galactic"
                24 -> "Nebulous"
                25 -> "Stellar"
                26 -> "Astral"
                27 -> "Ethereal"
                28 -> "Quantum"
                29 -> "Singularity"
                30 -> "Transcendent"
                31 -> "Apex"
                32 -> "Zenith"
                33 -> "Pinnacle"
                34 -> "Absolute"
                35 -> "Ultimate"
                36 -> "Omega"
                37 -> "Alpha"
                38 -> "Genesis"
                39 -> "Void"
                40 -> "Oblivion"
                else -> "Undefined"
            }
        }

        /**
         * Calculates the Rarity attribute for a token.
         *
         * This method combines three key traits — Level, Power, and Creation Date — into a single 320-bit number.
         * The resulting score is useful for sorting NFTs by rarity, as it provides a unique and comparable value
         * based on these traits.
         *
         * The calculation works as follows:
         * - The Level is shifted left by 320 bits to give it the highest weight.
         * - The Power is shifted left by 64 bits to give it a medium weight.
         * - The Creation Date is inverted (to prioritize older tokens) and added as the least significant part.
         *
         * Note: The 320-bit size of the score makes it incompatible with many NFT marketplaces, which typically
         * do not support such large numbers. As a result, this attribute is not currently included in the metadata.
         *
         * Example usage:
         * val rarity = calculateRarity(level, power, createdAt)
         * TokenMetadata.Attribute("Rarity", JsonUnquotedLiteral(rarity.toString(10)), displayType = "number")
         *
         * @param level The Level of the token, representing leading zero bits.
         * @param power The Power of the token, indicating the amount of VKEY tokens burned.
         * @param createdAt The creation timestamp of the token, in seconds since the epoch.
         * @return A BigInteger representing the combined rarity score.
         */
        private fun calculateRarity(
            level: UByte,
            power: BigInteger,
            createdAt: ULong,
        ): BigInteger {
            var score = BigInteger.valueOf(level.toLong()).shiftLeft(320)
            score = score.or(power.shiftLeft(64))
            score = score.or(BigInteger.valueOf(Long.MAX_VALUE - createdAt.toLong()))
            return score
        }

        private fun formatTokenDescriptionDate(createdAt: ULong): String {
            return tokenCreationDateFormatter.format(Instant.ofEpochSecond(createdAt.toLong()))
        }
    }
}