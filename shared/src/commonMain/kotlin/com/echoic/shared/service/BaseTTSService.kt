package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*

/**
 * Base class for TTS service implementations.
 * Provides common functionality for reading audio responses.
 */
abstract class BaseTTSService(
    protected val httpClient: HttpClient,
    protected val apiKey: String,
    protected val baseURL: String,
) : TTSService {

    protected suspend fun readAudioResponse(response: HttpResponse): ByteArray {
        val status = response.status
        if (!status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw TTSResponseException(
                "TTS API error ${status.value}: $errorBody"
            )
        }

        val channel = response.bodyAsChannel()
        val buffer = ByteArray(8192)
        val result = mutableListOf<Byte>()
        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer)
            if (bytesRead > 0) {
                for (i in 0 until bytesRead) result.add(buffer[i])
            }
        }

        val bytes = result.toByteArray()
        if (bytes.isEmpty()) {
            throw TTSResponseException("TTS returned empty audio response")
        }
        return bytes
    }
}
