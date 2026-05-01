package com.echoic.shared.config

import com.echoic.shared.model.TTSProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Application configuration stored as JSON.
 */
@Serializable
data class AppConfigData(
    val appearance: String = "system",     // "light", "dark", "system"
    val language: String = "en",           // "en", "zh"
    val providerKeys: Map<String, String> = emptyMap(),  // provider name -> API key
    val providerURLs: Map<String, String> = emptyMap(),  // provider name -> custom base URL
    val hasCompletedOnboarding: Boolean = false,
    val defaultOutputFormat: String = "mp3",
    val saveDirectory: String = "",
)

/**
 * Manages app configuration with JSON file persistence.
 */
class AppConfig(private val configFile: File) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var _data: AppConfigData = load()

    val data: AppConfigData get() = _data

    fun getApiKey(provider: TTSProvider): String? {
        val key = _data.providerKeys[provider.name] ?: return null
        return key.ifBlank { null }
    }

    fun getBaseURL(provider: TTSProvider): String {
        return _data.providerURLs[provider.name] ?: provider.defaultBaseURL
    }

    fun setApiKey(provider: TTSProvider, key: String) {
        _data = _data.copy(
            providerKeys = _data.providerKeys + (provider.name to key)
        )
        save()
    }

    fun setBaseURL(provider: TTSProvider, url: String) {
        _data = _data.copy(
            providerURLs = _data.providerURLs + (provider.name to url)
        )
        save()
    }

    fun updateAppearance(appearance: String) {
        _data = _data.copy(appearance = appearance)
        save()
    }

    fun updateLanguage(language: String) {
        _data = _data.copy(language = language)
        save()
    }

    fun completeOnboarding() {
        _data = _data.copy(hasCompletedOnboarding = true)
        save()
    }

    fun isProviderConfigured(provider: TTSProvider): Boolean {
        return getApiKey(provider) != null
    }

    private fun load(): AppConfigData {
        return try {
            if (configFile.exists()) {
                json.decodeFromString<AppConfigData>(configFile.readText())
            } else {
                AppConfigData()
            }
        } catch (_: Exception) {
            AppConfigData()
        }
    }

    private fun save() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(AppConfigData.serializer(), _data))
        } catch (_: Exception) {
            // Silently fail — config is not critical
        }
    }

    companion object {
        fun defaultPath(): File {
            val os = System.getProperty("os.name").lowercase()
            val home = System.getProperty("user.home")
            val configDir = when {
                os.contains("mac") -> "$home/Library/Application Support/echoic"
                os.contains("win") -> "${System.getenv("APPDATA")}/echoic"
                else -> "$home/.config/echoic"
            }
            return File(configDir, "config.json")
        }
    }
}
