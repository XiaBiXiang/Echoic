package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Fish Audio TTS service.
 * API: POST /v1/audio/speech (OpenAI-compatible)
 */
class FishAudioService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.FISH_AUDIO.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TTSRequest(
        val model: String,
        val input: String,
        val voice: String = "Default",
        val response_format: String = "mp3",
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/v1/audio/speech"

        val response = httpClient.post(url) {
            header("Authorization", "Bearer $apiKey")
            accept(ContentType.parse(format.mimeType))
            contentType(ContentType.Application.Json)
            setBody(
                TTSRequest(
                    model = model.rawValue,
                    input = text,
                    voice = voice.id,
                    response_format = format.rawValue,
                )
            )
        }

        return readAudioResponse(response)
    }
}
