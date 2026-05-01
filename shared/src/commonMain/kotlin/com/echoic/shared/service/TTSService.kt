package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.Voice

/**
 * Provider-specific TTS service interface.
 * Each provider (Fish Audio, etc.) implements this.
 */
interface TTSService {
    suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat,
    ): ByteArray
}
