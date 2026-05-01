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
 * Baidu TTS service.
 * API: POST /text2audio
 */
class BaiduTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.BAIDU.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/text2audio"

        val formatParam = when (format) {
            AudioFormat.MP3 -> "mp3"
            AudioFormat.WAV -> "wav"
            AudioFormat.OPUS -> "opus"
            AudioFormat.FLAC -> "pcm"
            AudioFormat.AAC -> "mp3"
        }

        val response = httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.parse(format.mimeType))
            setBody(
                listOf(
                    "tex" to text,
                    "tok" to apiKey,
                    "cuid" to "echoic-kmp",
                    "ctp" to "1",
                    "lan" to "zh",
                    "spd" to "5",
                    "pit" to "5",
                    "vol" to "5",
                    "per" to voice.id,
                    "aue" to formatParam,
                ).formUrlEncode()
            )
        }

        return readAudioResponse(response)
    }
}
