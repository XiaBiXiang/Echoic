package com.echoic.shared.engine

import com.echoic.shared.config.AppConfig
import com.echoic.shared.model.TTSProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating TTSEngine instances.
 * Manages HttpClient lifecycle.
 */
object TTSEngineFactory {
    fun create(config: AppConfig): CloudTTSEngine {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }

        return CloudTTSEngine(
            httpClient = httpClient,
            apiKeyProvider = { provider -> config.getApiKey(provider) },
            baseURLProvider = { provider -> config.getBaseURL(provider) },
        )
    }
}
