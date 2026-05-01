package com.echoic.shared.engine

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.Voice

/**
 * Unified interface for TTS synthesis.
 * Implementations can be cloud-based or local.
 */
interface TTSEngine {
    /**
     * Synthesize text to audio data.
     * @return audio bytes in the requested format
     */
    suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat = AudioFormat.MP3,
    ): ByteArray

    /**
     * Cancel any ongoing synthesis.
     */
    fun cancel()
}
