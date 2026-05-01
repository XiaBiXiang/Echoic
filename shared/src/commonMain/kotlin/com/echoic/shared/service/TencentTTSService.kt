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
 * Tencent Cloud TTS service.
 * API: POST / (Tencent Cloud API)
 */
class TencentTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.TENCENT.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TTSRequest(
        val Text: String,
        val VoiceType: Int,
        val Codec: String = "mp3",
        val SampleRate: Int = 16000,
        val Volume: Int = 0,
        val Speed: Double = 0.0,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = baseURL.trimEnd('/')

        val codec = when (format) {
            AudioFormat.MP3 -> "mp3"
            AudioFormat.WAV -> "wav"
            AudioFormat.OPUS -> "ogg-opus"
            AudioFormat.FLAC -> "flac"
            AudioFormat.AAC -> "mp3"
        }

        val voiceType = voice.id.toIntOrNull() ?: 101001

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.parse(format.mimeType))
            header("X-TC-Action", "TextToVoice")
            header("X-TC-Version", "2019-08-23")
            header("X-TC-Region", "ap-guangzhou")
            header("Authorization", "TC3-HMAC-SHA256 Credential=$apiKey")
            setBody(
                TTSRequest(
                    Text = text,
                    VoiceType = voiceType,
                    Codec = codec,
                )
            )
        }

        return readAudioResponse(response)
    }
}
