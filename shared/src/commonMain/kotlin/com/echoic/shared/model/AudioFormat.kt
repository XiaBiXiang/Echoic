package com.echoic.shared.model

enum class AudioFormat(
    val rawValue: String,
    val displayName: String,
    val fileExtension: String,
    val mimeType: String,
) {
    MP3("mp3", "MP3", "mp3", "audio/mpeg"),
    WAV("wav", "WAV", "wav", "audio/wav"),
    OPUS("opus", "OPUS", "opus", "audio/ogg"),
    FLAC("flac", "FLAC", "flac", "audio/flac"),
    AAC("aac", "AAC", "aac", "audio/aac"),
}
