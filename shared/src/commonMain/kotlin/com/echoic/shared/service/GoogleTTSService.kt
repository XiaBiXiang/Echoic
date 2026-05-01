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
 * Google Cloud TTS service.
 * API: POST /v1/text:synthesize
 */
class GoogleTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.GOOGLE.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class VoiceSelectionParams(
        val languageCode: String = "en-US",
        val name: String,
    )

    @Serializable
    private data class AudioConfig(
        val audioEncoding: String = "MP3",
    )

    @Serializable
    private data class TTSRequest(
        val input: SynthesisInput,
        val voice: VoiceSelectionParams,
        val audioConfig: AudioConfig,
    )

    @Serializable
    private data class SynthesisInput(
        val text: String,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/v1/text:synthesize?key=$apiKey"

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.parse(format.mimeType))
            setBody(
                TTSRequest(
                    input = SynthesisInput(text = text),
                    voice = VoiceSelectionParams(name = model.rawValue),
                    audioConfig = AudioConfig(audioEncoding = format.rawValue.uppercase()),
                )
            )
        }

        return readAudioResponse(response)
    }
}
