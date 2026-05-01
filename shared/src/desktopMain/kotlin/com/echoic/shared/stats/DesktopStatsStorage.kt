package com.echoic.shared.stats

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Desktop implementation of [StatsStorage] using JSON file persistence.
 * Stores data at ~/.echoic/usage_stats.json.
 */
actual class StatsStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val statsFile: File by lazy {
        val homeDir = System.getProperty("user.home")
        val echoicDir = File(homeDir, ".echoic")
        if (!echoicDir.exists()) {
            echoicDir.mkdirs()
        }
        File(echoicDir, "usage_stats.json")
    }

    actual fun load(): UsageStats {
        return try {
            if (statsFile.exists()) {
                val content = statsFile.readText()
                if (content.isNotBlank()) {
                    json.decodeFromString<UsageStats>(content)
                } else {
                    UsageStats()
                }
            } else {
                UsageStats()
            }
        } catch (_: Exception) {
            UsageStats()
        }
    }

    actual fun save(stats: UsageStats) {
        try {
            val parent = statsFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            statsFile.writeText(json.encodeToString(stats))
        } catch (_: Exception) {
            // Silently fail - stats are non-critical
        }
    }

    actual fun delete() {
        try {
            if (statsFile.exists()) {
                statsFile.delete()
            }
        } catch (_: Exception) {
            // Silently fail
        }
    }
}
