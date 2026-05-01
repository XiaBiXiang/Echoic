package com.echoic.shared.util

/**
 * Cross-platform base64 decoding.
 */
expect fun base64Decode(encoded: String): ByteArray

/**
 * Decode a hex string to bytes.
 */
fun hexDecode(hex: String): ByteArray {
    val bytes = ByteArray(hex.length / 2)
    for (i in bytes.indices) {
        bytes[i] = ((hexDigit(hex[i * 2]) shl 4) or hexDigit(hex[i * 2 + 1])).toByte()
    }
    return bytes
}

private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> throw IllegalArgumentException("Invalid hex character: $c")
}
