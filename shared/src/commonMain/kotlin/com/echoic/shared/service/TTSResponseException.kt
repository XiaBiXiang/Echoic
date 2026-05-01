package com.echoic.shared.service

/**
 * Exception thrown when a TTS API returns an error response.
 */
class TTSResponseException(message: String) : Exception(message)
