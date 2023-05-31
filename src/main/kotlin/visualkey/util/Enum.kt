package visualkey.util

inline fun <reified T : Enum<T>> enumIgnoreCaseValueOf(name: String? = null): T? {
    if (name == null) {
        return null
    }

    return enumValues<T>().find { it.name.equals(name, ignoreCase = true) }
}
