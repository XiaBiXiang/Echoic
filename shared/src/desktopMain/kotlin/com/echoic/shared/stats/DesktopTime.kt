package com.echoic.shared.stats

/**
 * Desktop (JVM) actual implementation of [currentTimeMillisImpl].
 */
actual fun currentTimeMillisImpl(): Long = System.currentTimeMillis()
