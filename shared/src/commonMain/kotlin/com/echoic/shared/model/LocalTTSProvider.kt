package com.echoic.shared.model

/**
 * Represents a download mirror for TTS models.
 *
 * @param name Display name of the mirror (e.g., "HuggingFace", "hf-mirror.com", "GitHub")
 * @param url Direct URL to download or browse the model
 * @param isDefault Whether this mirror should be used as the default download source
 */
data class DownloadMirror(
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
)

/**
 * Local TTS providers that run entirely on-device without requiring API keys or internet.
 */
enum class LocalTTSProvider(
    val displayName: String,
    val subtitle: String,
    val downloadURL: String?,
    val githubURL: String?,
    val documentationURL: String?,
    val tags: List<TTSTag>,
    val modelSizeMB: Int?,
    val supportedLanguages: List<String>,
    val integrationMethod: String,
    val notes: String,
    val platformSupport: List<Platform>,
    val downloadMirrors: List<DownloadMirror> = emptyList(),
    val requiresGPU: Boolean = false,
    val minVRAM: Int? = null, // GB
) {
    PIPER(
        displayName = "Piper",
        subtitle = "Fast, lightweight neural TTS (ONNX-based)",
        downloadURL = "https://huggingface.co/rhasspy/piper-voices",
        githubURL = "https://github.com/rhasspy/piper",
        documentationURL = "https://rhasspy.github.io/piper/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.LIGHTWEIGHT,
            TTSTag.FAST,
            TTSTag.MULTILINGUAL,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 60,
        supportedLanguages = listOf("en", "zh", "de", "fr", "es", "it", "nl", "pl", "ru", "ja", "ko"),
        integrationMethod = "ONNX Runtime Java bindings or JNI",
        notes = "Based on VITS architecture. Models in ONNX format. Very fast inference. Community-maintained voice models for 30+ languages.",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.LINUX_ARM64,
            Platform.LINUX_ARMV7,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
        ),
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx",
            ),
            DownloadMirror(
                name = "hf-mirror (XiaoYa Chinese)",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/zh/zh_CN/xiao_ya/medium/zh_CN-xiao_ya-medium.onnx",
            ),
            DownloadMirror(
                name = "HuggingFace (XiaoYa Chinese)",
                url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/zh/zh_CN/xiao_ya/medium/zh_CN-xiao_ya-medium.onnx",
            ),
        ),
    ),
    SHERPA(
        displayName = "Sherpa-ONNX",
        subtitle = "Cross-platform speech toolkit with Java bindings",
        downloadURL = "https://github.com/k2-fsa/sherpa-onnx/releases",
        githubURL = "https://github.com/k2-fsa/sherpa-onnx",
        documentationURL = "https://k2-fsa.github.io/sherpa/onnx/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.LIGHTWEIGHT,
            TTSTag.FAST,
            TTSTag.MULTILINGUAL,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 115,
        supportedLanguages = listOf("en", "zh", "de", "fr", "es", "ja", "ko", "ru"),
        integrationMethod = "JNI Java bindings (official)",
        notes = "Official Java/Kotlin bindings via JNI. Supports multiple TTS models (VITS, Piper, etc.). Best option for JVM/KMP integration. Also supports ASR and speaker identification.",
        platformSupport = listOf(
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
            Platform.WINDOWS_X64,
            Platform.WINDOWS_ARM64,
            Platform.LINUX_X64,
            Platform.LINUX_ARM64,
            Platform.LINUX_ARMV7,
            Platform.ANDROID_ARM64,
            Platform.ANDROID_ARMV7,
            Platform.IOS_ARM64,
            Platform.WEB_WASM,
        ),
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-unity/resolve/main/vits-zh-hf-fanchen-unity.onnx",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/csukuangfj/vits-zh-hf-fanchen-unity/resolve/main/vits-zh-hf-fanchen-unity.onnx",
            ),
            DownloadMirror(
                name = "hf-mirror (MeloTTS Chinese-English)",
                url = "https://hf-mirror.com/csukuangfj/vits-melo-tts-zh_en/resolve/main/model.onnx",
            ),
            DownloadMirror(
                name = "hf-mirror (Piper Chinese Medium)",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx",
            ),
        ),
    ),
    ESPEAK(
        displayName = "eSpeak NG",
        subtitle = "Lightweight, multilingual speech synthesizer",
        downloadURL = "https://github.com/espeak-ng/espeak-ng/releases",
        githubURL = "https://github.com/espeak-ng/espeak-ng",
        documentationURL = null,
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.LIGHTWEIGHT,
            TTSTag.FAST,
            TTSTag.MULTILINGUAL,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
        ),
        modelSizeMB = 10,
        supportedLanguages = listOf("en", "zh", "de", "fr", "es", "it", "pt", "ru", "ja", "ko", "ar", "hi"),
        integrationMethod = "JNI/JNA wrapper or ProcessBuilder",
        notes = "Supports 80+ languages. Very small footprint. Formant-based synthesis (less natural than neural models). Good fallback option.",
        platformSupport = listOf(
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
            Platform.WINDOWS_X64,
            Platform.WINDOWS_ARM64,
            Platform.LINUX_X64,
            Platform.LINUX_ARM64,
            Platform.LINUX_ARMV7,
        ),
        downloadMirrors = listOf(
            DownloadMirror(
                name = "Windows Installer (MSI)",
                url = "https://github.com/espeak-ng/espeak-ng/releases/download/1.52.0/espeak-ng.msi",
                isDefault = true,
            ),
            DownloadMirror(
                name = "Source Code",
                url = "https://github.com/espeak-ng/espeak-ng/archive/refs/tags/1.52.0.zip",
            ),
        ),
    ),
    VOXCPM(
        displayName = "VoxCPM",
        subtitle = "Tokenizer-free TTS with voice cloning (OpenBMB)",
        downloadURL = "https://huggingface.co/openbmb/VoxCPM2",
        githubURL = "https://github.com/OpenBMB/VoxCPM",
        documentationURL = "https://voxcpm.readthedocs.io/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.VOICE_CLONING,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 4700,
        supportedLanguages = listOf("zh", "en", "ja", "ko", "de", "fr", "es", "it", "ru", "ar", "hi", "th", "vi", "id"),
        integrationMethod = "Python API or REST API",
        notes = "VoxCPM2: 2B参数模型，支持30种语言，可从自然语言描述创建新声音，支持声音克隆。Apache-2.0开源协议。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
            Platform.MACOS_ARM64,
        ),
        requiresGPU = true,
        minVRAM = 8,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/openbmb/VoxCPM2/resolve/main/model.safetensors",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/openbmb/VoxCPM2/resolve/main/model.safetensors",
            ),
        ),
    ),
    COSYVOICE(
        displayName = "CosyVoice",
        subtitle = "Multi-lingual TTS with voice cloning (Alibaba)",
        downloadURL = "https://huggingface.co/FunAudioLLM/CosyVoice2-0.5B",
        githubURL = "https://github.com/FunAudioLLM/CosyVoice",
        documentationURL = "https://fun-audio-llm.github.io/CosyVoice/",
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.VOICE_CLONING,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 3200,
        supportedLanguages = listOf("zh", "en", "ja", "ko", "de", "fr", "es", "it", "ru"),
        integrationMethod = "Python API or Gradio Web UI",
        notes = "Fun-CosyVoice 3.0：支持9种语言和18种中文方言，零样本声音克隆，拼音/音素控制，流式合成延迟低至150ms。Apache-2.0开源协议。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
        ),
        requiresGPU = true,
        minVRAM = 4,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/FunAudioLLM/CosyVoice2-0.5B/resolve/main/llm.pt",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/FunAudioLLM/CosyVoice2-0.5B/resolve/main/llm.pt",
            ),
        ),
    ),
    CHATTTS(
        displayName = "ChatTTS",
        subtitle = "Conversational TTS for daily dialogue",
        downloadURL = "https://huggingface.co/2noise/ChatTTS",
        githubURL = "https://github.com/2noise/ChatTTS",
        documentationURL = null,
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.FAST,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 1200,
        supportedLanguages = listOf("zh", "en"),
        integrationMethod = "Python API",
        notes = "专为日常对话设计的生成式语音模型，支持中英文，可与LLM集成。39k+ GitHub Stars，社区活跃。Apache-2.0开源协议。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
            Platform.MACOS_X64,
        ),
        requiresGPU = false,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/2noise/ChatTTS/resolve/main/asset/gpt/model.safetensors",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/2noise/ChatTTS/resolve/main/asset/gpt/model.safetensors",
            ),
        ),
    ),
    GPTSOVITS(
        displayName = "GPT-SoVITS",
        subtitle = "Few-shot voice cloning TTS",
        downloadURL = "https://huggingface.co/lj1995/GPT-SoVITS",
        githubURL = "https://github.com/RVC-Boss/GPT-SoVITS",
        documentationURL = null,
        tags = listOf(
            TTSTag.OFFLINE,
            TTSTag.OPEN_SOURCE,
            TTSTag.MULTILINGUAL,
            TTSTag.HIGH_QUALITY,
            TTSTag.VOICE_CLONING,
            TTSTag.LOCAL_COMPUTE,
            TTSTag.NO_API_KEY,
            TTSTag.NEURAL,
        ),
        modelSizeMB = 2000,
        supportedLanguages = listOf("zh", "en", "ja", "ko"),
        integrationMethod = "Python API or Web UI",
        notes = "最流行的开源TTS项目（57k+ Stars），仅需1分钟语音数据即可训练高质量TTS模型。支持中英日韩四语。MIT开源协议。",
        platformSupport = listOf(
            Platform.LINUX_X64,
            Platform.WINDOWS_X64,
        ),
        requiresGPU = true,
        minVRAM = 4,
        downloadMirrors = listOf(
            DownloadMirror(
                name = "hf-mirror.com (Recommended)",
                url = "https://hf-mirror.com/lj1995/GPT-SoVITS/resolve/main/s2G488k.pth",
                isDefault = true,
            ),
            DownloadMirror(
                name = "HuggingFace",
                url = "https://huggingface.co/lj1995/GPT-SoVITS/resolve/main/s2G488k.pth",
            ),
        ),
    ),
    ;

    val requiresAPIKey: Boolean get() = false
    val isLocal: Boolean get() = true

    /** Check if this provider has a specific tag. */
    fun hasTag(tag: TTSTag): Boolean = tag in tags

    /** Check if this provider has all of the given tags. */
    fun hasAllTags(vararg tagList: TTSTag): Boolean = tagList.all { it in tags }

    /** Check if this provider has any of the given tags. */
    fun hasAnyTag(vararg tagList: TTSTag): Boolean = tagList.any { it in tags }

    /** Check if this provider supports a given language code. */
    fun supportsLanguage(languageCode: String): Boolean =
        languageCode in supportedLanguages

    /** Available local models for this provider. */
    val availableModels: List<LocalTTSModel>
        get() = LocalTTSModel.entries.filter { it.provider == this }
}
