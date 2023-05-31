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

enum class ChainRegion {
    Local,
    Testnet,
    Mainnet,
}

fun String.toChainRegion(): ChainRegion {
    return enumIgnoreCaseValueOf<ChainRegion>(this)
        ?: throw IllegalArgumentException("Unknown chain environment: $this")
}
