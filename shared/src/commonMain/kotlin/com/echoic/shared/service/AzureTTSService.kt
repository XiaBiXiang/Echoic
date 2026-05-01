package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Azure Cognitive Services TTS service.
 * API: POST /cognitiveservices/v1
 */
class AzureTTSService(
    httpClient: HttpClient,
    apiKey: String,
    baseURL: String = TTSProvider.AZURE.defaultBaseURL,
) : BaseTTSService(httpClient, apiKey, baseURL) {

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray {
        val url = "${baseURL.trimEnd('/')}/cognitiveservices/v1"

        val ssml = """
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>
                <voice name='${model.rawValue}'>
                    $text
                </voice>
            </speak>
        """.trimIndent()

        val audioFormat = when (format) {
            AudioFormat.MP3 -> "audio-24khz-160kbitrate-mono-mp3"
            AudioFormat.WAV -> "riff-24khz-16bit-mono-pcm"
            AudioFormat.OPUS -> "ogg-24khz-16bit-mono-opus"
            AudioFormat.FLAC -> "audio-24khz-96kbitrate-mono-flac"
            AudioFormat.AAC -> "audio-24khz-160kbitrate-mono-mp3"
        }

        val response = httpClient.post(url) {
            header("Ocp-Apim-Subscription-Key", apiKey)
            contentType(ContentType.Application.Xml)
            accept(ContentType.parse(format.mimeType))
            header("X-Microsoft-OutputFormat", audioFormat)
            setBody(ssml)
        }

        return readAudioResponse(response)
    }
}
