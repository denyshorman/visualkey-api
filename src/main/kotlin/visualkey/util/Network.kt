package visualkey.util

import io.ktor.http.*

fun RequestConnectionPoint.serverUrl(): String {
    return if (serverPort == 80 || serverPort == 443) {
        "$scheme://$serverHost"
    } else {
        "$scheme://$serverHost:$serverPort"
    }
}
