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
 * Alibaba Cloud NLS TTS service.
 * API: POST /stream/v1/tts
 */
class AliyunTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.ALIYUN.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TTSRequest(
        val appkey: String,
        val text: String,
        val voice: String = "xiaoyun",
        val format: String = "mp3",
        val sample_rate: Int = 16000,
        val volume: Int = 50,
        val speech_rate: Int = 0,
        val pitch_rate: Int = 0,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/stream/v1/tts"

        val formatParam = when (format) {
            AudioFormat.MP3 -> "mp3"
            AudioFormat.WAV -> "wav"
            AudioFormat.OPUS -> "opus"
            AudioFormat.FLAC -> "pcm"
            AudioFormat.AAC -> "mp3"
        }

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.parse(format.mimeType))
            header("X-NLS-Token", apiKey)
            setBody(
                TTSRequest(
                    appkey = apiKey,
                    text = text,
                    voice = voice.id,
                    format = formatParam,
                )
            )
        }

        return readAudioResponse(response)
    }
}
