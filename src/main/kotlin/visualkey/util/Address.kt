package visualkey.util

infix fun String.addrEq(other: String?): Boolean {
    return equals(other, ignoreCase = true)
}

infix fun String.addrNe(other: String?): Boolean {
    return !addrEq(other)
}
