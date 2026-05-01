package com.echoic.shared.stats

/**
 * Cross-platform storage for usage statistics.
 * Platform-specific implementations via expect/actual.
 */
expect class StatsStorage {
    /**
     * Load usage stats from persistent storage.
     * Returns a default [UsageStats] if no saved data exists.
     */
    fun load(): UsageStats

    /**
     * Save usage stats to persistent storage.
     */
    fun save(stats: UsageStats)

    /**
     * Delete the stored stats file, effectively resetting all data.
     */
    fun delete()
}
