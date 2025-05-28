package visualkey.service.nft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CollectionMetadata(
    // The name of the contract.
    val name: String,

    // The symbol of the contract.
    val symbol: String,

    // The description of the contract.
    val description: String,

    // A URI pointing to a resource with mime type image/* that represents the contract, typically displayed as a profile picture for the contract
    val image: String,

    // A URI pointing to a resource with mime type image/* that represents the contract, displayed as a banner image for the contract.
    @SerialName("banner_image") val bannerImage: String,

    // A URI pointing to a resource with mime type image/* that represents the featured image for the contract, typically used for a highlight section.
    @SerialName("featured_image") val featuredImage: String,

    // The external link of the contract.
    @SerialName("external_link") val externalLink: String,

    // An array of Ethereum addresses representing collaborators (authorized editors) of the contract.
    val collaborators: List<String>,
)

@Serializable
data class TokenMetadata(
    val name: String,
    val description: String,
    val image: String,
    @SerialName("external_url") val externalUrl: String,
    @SerialName("background_color") val bgColor: String,
    val attributes: List<Attribute>,
) {
    @Serializable
    data class Attribute(
        @SerialName("trait_type") val traitType: String,
        val value: JsonElement,
        @SerialName("display_type") val displayType: String? = null
    )
}