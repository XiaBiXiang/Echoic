package com.echoic.shared.model

enum class TTSTag(
    val enName: String,
    val zhName: String,
) {
    FREE(enName = "Free Tier", zhName = "免费额度"),
    CHINESE(enName = "Chinese", zhName = "中文效果好"),
    ENGLISH(enName = "English", zhName = "英文效果好"),
    MULTILINGUAL(enName = "Multilingual", zhName = "多语言支持"),
    HIGH_QUALITY(enName = "High Quality", zhName = "高质量"),
    FAST(enName = "Fast", zhName = "速度快"),
    VOICE_CLONING(enName = "Voice Cloning", zhName = "语音克隆"),
    LOCAL(enName = "Domestic", zhName = "国内服务商"),
    INTERNATIONAL(enName = "International", zhName = "国际服务商"),

    // Local TTS specific tags
    OFFLINE(enName = "Offline", zhName = "离线可用"),
    OPEN_SOURCE(enName = "Open Source", zhName = "开源"),
    LIGHTWEIGHT(enName = "Lightweight", zhName = "轻量级"),
    LOCAL_COMPUTE(enName = "Local Compute", zhName = "本地计算"),
    NO_API_KEY(enName = "No API Key", zhName = "无需 API Key"),
    NEURAL(enName = "Neural", zhName = "神经网络"),
    ;

    fun displayName(language: String): String = when (language) {
        "zh" -> zhName
        else -> enName
    }
}
