package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import com.echoic.shared.util.hexDecode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * MiniMax TTS service.
 * API: POST /v1/t2a_v2
 */
class MiniMaxTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.MINIMAX.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class VoiceSetting(
        val voice_id: String,
        val speed: Double = 1.0,
        val vol: Double = 1.0,
        val pitch: Int = 0,
    )

    @Serializable
    private data class AudioSetting(
        val sample_rate: Int = 32000,
        val bitrate: Int = 128000,
        val format: String = "mp3",
        val channel: Int = 1,
    )

    @Serializable
    private data class TTSRequest(
        val model: String,
        val text: String,
        val voice_setting: VoiceSetting,
        val audio_setting: AudioSetting = AudioSetting(),
    )

    @Serializable
    private data class TTSResponse(
        val data: TTSResponseData? = null,
        val base_resp: BaseResp? = null,
    )

    @Serializable
    private data class TTSResponseData(
        val audio: String? = null,
    )

    @Serializable
    private data class BaseResp(
        val status_code: Int = -1,
        val status_msg: String? = null,
    )

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/v1/t2a_v2"

        val formatParam = when (format) {
            AudioFormat.MP3 -> "mp3"
            AudioFormat.WAV -> "wav"
            AudioFormat.OPUS -> "mp3"
            AudioFormat.FLAC -> "mp3"
            AudioFormat.AAC -> "mp3"
        }

        val response = httpClient.post(url) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                TTSRequest(
                    model = model.rawValue,
                    text = text,
                    voice_setting = VoiceSetting(voice_id = voice.id),
                    audio_setting = AudioSetting(format = formatParam),
                )
            )
        }

        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw TTSResponseException("MiniMax API error ${response.status.value}: $responseBody")
        }

        val ttsResponse = json.decodeFromString<TTSResponse>(responseBody)

        // Check for API-level errors
        val statusCode = ttsResponse.base_resp?.status_code ?: -1
        if (statusCode != 0 && statusCode != 1002) {
            throw TTSResponseException(
                "MiniMax API error $statusCode: ${ttsResponse.base_resp?.status_msg ?: "Unknown error"}"
            )
        }

        // Decode hex-encoded audio data
        val audioHex = ttsResponse.data?.audio
            ?: throw TTSResponseException("MiniMax returned empty audio data")

        return hexDecode(audioHex)
    }
}
