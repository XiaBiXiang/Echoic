package com.echoic.shared.stats

import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import kotlinx.serialization.Serializable

/**
 * Data class representing usage statistics for TTS providers.
 */
@Serializable
data class UsageStats(
    /** Total number of TTS usage sessions */
    val totalUsageCount: Int = 0,
    /** Total characters processed across all providers */
    val totalCharacters: Int = 0,
    /** Usage count per cloud provider */
    val providerUsageCount: Map<String, Int> = emptyMap(),
    /** Usage count per local provider */
    val localProviderUsageCount: Map<String, Int> = emptyMap(),
    /** Name of the last used provider (cloud or local) */
    val lastUsedProvider: String? = null,
    /** Timestamp of the last usage */
    val lastUsedTime: Long? = null,
) {
    /**
     * Get the most used cloud TTS provider.
     * Returns null if no cloud provider has been used.
     */
    fun getMostUsedProvider(): TTSProvider? {
        val maxEntry = providerUsageCount.maxByOrNull { it.value } ?: return null
        return try {
            TTSProvider.valueOf(maxEntry.key)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Get the most used local TTS provider.
     * Returns null if no local provider has been used.
     */
    fun getMostUsedLocalProvider(): LocalTTSProvider? {
        val maxEntry = localProviderUsageCount.maxByOrNull { it.value } ?: return null
        return try {
            LocalTTSProvider.valueOf(maxEntry.key)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
