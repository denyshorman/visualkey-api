package visualkey.api.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val params: JsonObject? = null,
)
