package com.echoic.shared.stats

import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for tracking and querying TTS usage statistics.
 * Persists data via [StatsStorage].
 */
class UsageStatsManager(
    private val storage: StatsStorage,
) {
    private val _stats = MutableStateFlow(storage.load())
    /** Observable usage stats */
    val stats: StateFlow<UsageStats> = _stats.asStateFlow()

    /**
     * Record a usage of a cloud TTS provider.
     *
     * @param provider   The cloud provider used
     * @param characters Number of characters processed
     */
    fun recordUsage(provider: TTSProvider, characters: Int) {
        val current = _stats.value
        val updatedProviderCounts = current.providerUsageCount.toMutableMap()
        updatedProviderCounts[provider.name] = (updatedProviderCounts[provider.name] ?: 0) + 1

        val updated = current.copy(
            totalUsageCount = current.totalUsageCount + 1,
            totalCharacters = current.totalCharacters + characters,
            providerUsageCount = updatedProviderCounts,
            lastUsedProvider = provider.name,
            lastUsedTime = currentTimeMillisImpl(),
        )
        persist(updated)
    }

    /**
     * Record a usage of a local TTS provider.
     *
     * @param localProvider The local provider used
     * @param characters    Number of characters processed
     */
    fun recordLocalUsage(localProvider: LocalTTSProvider, characters: Int) {
        val current = _stats.value
        val updatedLocalCounts = current.localProviderUsageCount.toMutableMap()
        updatedLocalCounts[localProvider.name] = (updatedLocalCounts[localProvider.name] ?: 0) + 1

        val updated = current.copy(
            totalUsageCount = current.totalUsageCount + 1,
            totalCharacters = current.totalCharacters + characters,
            localProviderUsageCount = updatedLocalCounts,
            lastUsedProvider = localProvider.name,
            lastUsedTime = currentTimeMillisImpl(),
        )
        persist(updated)
    }

    /**
     * Get the current usage statistics snapshot.
     */
    fun getStats(): UsageStats = _stats.value

    /**
     * Reset all usage statistics to zero.
     */
    fun resetStats() {
        val empty = UsageStats()
        persist(empty)
    }

    /**
     * Get the most used cloud TTS provider.
     * Returns null if no cloud provider has been used.
     */
    fun getMostUsedProvider(): TTSProvider? = _stats.value.getMostUsedProvider()

    /**
     * Get the most used local TTS provider.
     * Returns null if no local provider has been used.
     */
    fun getMostUsedLocalProvider(): LocalTTSProvider? = _stats.value.getMostUsedLocalProvider()

    // -- internal --

    private fun persist(updated: UsageStats) {
        _stats.value = updated
        storage.save(updated)
    }
}

/**
 * Platform helper for obtaining current epoch milliseconds.
 * Actual implementations are provided per target.
 */
expect fun currentTimeMillisImpl(): Long
