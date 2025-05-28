package visualkey.model

import visualkey.util.enumIgnoreCaseValueOf

enum class Environment {
    Local,
    Prod,
}

fun String.toEnvironment(): Environment {
    return enumIgnoreCaseValueOf<Environment>(this)
        ?: throw IllegalArgumentException("Unknown environment: $this")
}
