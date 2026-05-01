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
 * ElevenLabs TTS service.
 * API: POST /v1/text-to-speech/{voice_id}
 */
class ElevenLabsTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.ELEVENLABS.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TTSRequest(
        val text: String,
        val model_id: String,
        val voice_settings: VoiceSettings = VoiceSettings(),
    )

    @Serializable
    private data class VoiceSettings(
        val stability: Double = 0.5,
        val similarity_boost: Double = 0.75,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/v1/text-to-speech/${voice.id}"

        val outputFormat = when (format) {
            AudioFormat.MP3 -> "mp3_44100_128"
            AudioFormat.WAV -> "pcm_16000"
            AudioFormat.OPUS -> "opus_44100_128"
            AudioFormat.FLAC -> "pcm_16000"
            AudioFormat.AAC -> "mp3_44100_128"
        }

        val response = httpClient.post(url) {
            header("xi-api-key", apiKey)
            contentType(ContentType.Application.Json)
            accept(ContentType.parse(format.mimeType))
            parameter("output_format", outputFormat)
            setBody(
                TTSRequest(
                    text = text,
                    model_id = model.rawValue,
                )
            )
        }

        return readAudioResponse(response)
    }
}
