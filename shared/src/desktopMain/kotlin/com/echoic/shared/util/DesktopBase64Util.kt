package com.echoic.shared.util

import java.util.Base64

actual fun base64Decode(encoded: String): ByteArray {
    return Base64.getDecoder().decode(encoded)
}
