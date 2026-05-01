package com.echoic.shared.model

/**
 * Local TTS models that can run on-device.
 * Each model is associated with a LocalTTSProvider.
 */
enum class LocalTTSModel(
    val rawValue: String,
    val displayName: String,
    val provider: LocalTTSProvider,
    val language: String,
    val modelSizeMB: Int?,
    val tags: List<TTSTag>,
    val modelURL: String?,
    val notes: String,
) {
    // Piper models
    PIPER_EN_US_MEDIUM(
        rawValue = "en_US-amy-medium",
        displayName = "English (US) - Amy Medium",
        provider = LocalTTSProvider.PIPER,
        language = "en",
        modelSizeMB = 60,
        tags = listOf(TTSTag.ENGLISH, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://huggingface.co/rhasspy/piper-voices/tree/main/en/en_US/amy/medium",
        notes = "Medium quality English voice, good balance of speed and quality.",
    ),
    PIPER_ZH_CN_HUAYAN(
        rawValue = "zh_CN-huayan-medium",
        displayName = "Chinese (Mandarin) - Huayan Medium",
        provider = LocalTTSProvider.PIPER,
        language = "zh",
        modelSizeMB = 80,
        tags = listOf(TTSTag.CHINESE, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://huggingface.co/rhasspy/piper-voices/tree/main/zh/zh_CN/huayan/medium",
        notes = "Chinese Mandarin voice. Good quality for Chinese text.",
    ),
    PIPER_DE_THORSTEN(
        rawValue = "de_DE-thorsten-medium",
        displayName = "German - Thorsten Medium",
        provider = LocalTTSProvider.PIPER,
        language = "de",
        modelSizeMB = 60,
        tags = listOf(TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://huggingface.co/rhasspy/piper-voices/tree/main/de/de_DE/thorsten/medium",
        notes = "German male voice.",
    ),
    PIPER_FR_SIWISE(
        rawValue = "fr_FR-siwis-medium",
        displayName = "French - Siwis Medium",
        provider = LocalTTSProvider.PIPER,
        language = "fr",
        modelSizeMB = 60,
        tags = listOf(TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://huggingface.co/rhasspy/piper-voices/tree/main/fr/fr_FR/siwis/medium",
        notes = "French female voice.",
    ),

    // Sherpa-ONNX models
    SHERPA_VITS_ZH(
        rawValue = "vits-zh-aishell3",
        displayName = "VITS Chinese (AISHELL3)",
        provider = LocalTTSProvider.SHERPA,
        language = "zh",
        modelSizeMB = 80,
        tags = listOf(TTSTag.CHINESE, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Chinese TTS model optimized for Sherpa-ONNX. Fast inference.",
    ),
    SHERPA_VITS_EN(
        rawValue = "vits-en-vctk",
        displayName = "VITS English (VCTK)",
        provider = LocalTTSProvider.SHERPA,
        language = "en",
        modelSizeMB = 100,
        tags = listOf(TTSTag.ENGLISH, TTSTag.MULTILINGUAL, TTSTag.FAST),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Multi-speaker English TTS via Sherpa-ONNX.",
    ),
    SHERPA_PIPER_EN(
        rawValue = "piper-en-amy-medium",
        displayName = "Piper English (Amy)",
        provider = LocalTTSProvider.SHERPA,
        language = "en",
        modelSizeMB = 60,
        tags = listOf(TTSTag.ENGLISH, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Piper model running via Sherpa-ONNX runtime.",
    ),
    SHERPA_PIPER_ZH(
        rawValue = "piper-zh-huayan-medium",
        displayName = "Piper Chinese (Huayan)",
        provider = LocalTTSProvider.SHERPA,
        language = "zh",
        modelSizeMB = 80,
        tags = listOf(TTSTag.CHINESE, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = "https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models",
        notes = "Chinese Piper model running via Sherpa-ONNX runtime.",
    ),

    // eSpeak models
    ESPEAK_DEFAULT(
        rawValue = "espeak-ng-default",
        displayName = "eSpeak NG Default",
        provider = LocalTTSProvider.ESPEAK,
        language = "multi",
        modelSizeMB = 10,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.FAST, TTSTag.LIGHTWEIGHT),
        modelURL = null,
        notes = "Built-in eSpeak NG voices. 80+ languages. Very lightweight. Formant-based synthesis.",
    ),

    // VoxCPM models
    VOXCPM2(
        rawValue = "voxcpm2",
        displayName = "VoxCPM2 (2B)",
        provider = LocalTTSProvider.VOXCPM,
        language = "multi",
        modelSizeMB = 4000,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.VOICE_CLONING, TTSTag.NEURAL),
        modelURL = "https://huggingface.co/openbmb/VoxCPM2",
        notes = "VoxCPM2 2B参数模型，支持30种语言，声音克隆，声音设计。需要约8GB显存。",
    ),

    // CosyVoice models
    COSYVOICE2(
        rawValue = "cosyvoice2-0.5b",
        displayName = "CosyVoice2 (0.5B)",
        provider = LocalTTSProvider.COSYVOICE,
        language = "multi",
        modelSizeMB = 1000,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.VOICE_CLONING, TTSTag.NEURAL),
        modelURL = "https://github.com/FunAudioLLM/CosyVoice",
        notes = "Fun-CosyVoice 3.0，支持9种语言和18种中文方言，零样本声音克隆。",
    ),

    // ChatTTS models
    CHATTTS_DEFAULT(
        rawValue = "chattts-default",
        displayName = "ChatTTS Default",
        provider = LocalTTSProvider.CHATTTS,
        language = "multi",
        modelSizeMB = 800,
        tags = listOf(TTSTag.CHINESE, TTSTag.ENGLISH, TTSTag.HIGH_QUALITY, TTSTag.FAST, TTSTag.NEURAL),
        modelURL = "https://github.com/2noise/ChatTTS",
        notes = "专为日常对话设计，支持中英文，可与LLM集成。",
    ),

    // GPT-SoVITS models
    GPTSOVITS_V2(
        rawValue = "gpt-sovits-v2",
        displayName = "GPT-SoVITS v2",
        provider = LocalTTSProvider.GPTSOVITS,
        language = "multi",
        modelSizeMB = 2000,
        tags = listOf(TTSTag.MULTILINGUAL, TTSTag.HIGH_QUALITY, TTSTag.VOICE_CLONING, TTSTag.NEURAL),
        modelURL = "https://github.com/RVC-Boss/GPT-SoVITS",
        notes = "仅需1分钟语音数据即可训练高质量TTS模型，支持中英日韩四语。",
    ),
    ;

    /** Get the language display name. */
    val languageDisplayName: String
        get() = when (language) {
            "en" -> "English"
            "zh" -> "Chinese (Mandarin)"
            "de" -> "German"
            "fr" -> "French"
            "es" -> "Spanish"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            "sv" -> "Swedish"
            "te" -> "Telugu"
            "fi" -> "Finnish"
            "multi" -> "Multilingual"
            else -> language
        }

    /** Check if this model has a specific tag. */
    fun hasTag(tag: TTSTag): Boolean = tag in tags

    /** Check if this model has all of the given tags. */
    fun hasAllTags(vararg tagList: TTSTag): Boolean = tagList.all { it in tags }

    /** Check if this model has any of the given tags. */
    fun hasAnyTag(vararg tagList: TTSTag): Boolean = tagList.any { it in tags }
}
