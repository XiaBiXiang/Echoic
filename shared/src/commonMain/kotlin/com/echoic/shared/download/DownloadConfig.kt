package com.echoic.shared.download

import com.echoic.shared.model.LocalTTSProvider

/**
 * 下载配置
 * 定义各 TTS Provider 需要下载的文件列表，并提供生成完整下载 URL 的工具方法
 */
object DownloadConfig {
    // Piper TTS 下载文件列表（相对于 resolve/main 的路径）
    val PIPER_FILES = listOf(
        "zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx",
        "zh/zh_CN/huayan/medium/zh_CN-huayan-medium.onnx.json",
    )

    // Sherpa-ONNX 下载文件列表
    val SHERPA_FILES = listOf(
        "vits-zh-hf-fanchen-unity.onnx",
        "tokens.txt",
    )

    // eSpeak NG 下载文件列表
    val ESPEAK_FILES = listOf(
        "espeak-ng.msi",
    )

    // VoxCPM2 下载文件列表 (~4.7GB)
    val VOXCPM_FILES = listOf(
        "model.safetensors",
        "audiovae.pth",
    )

    // ChatTTS 下载文件列表 (~1.2GB)
    val CHATTTS_FILES = listOf(
        "asset/DVAE.safetensors",
        "asset/Decoder.safetensors",
        "asset/Embed.safetensors",
        "asset/gpt/model.safetensors",
        "asset/Vocos.safetensors",
    )

    // CosyVoice2 下载文件列表 (~3.2GB)
    val COSYVOICE_FILES = listOf(
        "llm.pt",
        "flow.pt",
        "hift.pt",
        "campplus.onnx",
        "speech_tokenizer_v2.onnx",
        "flow.decoder.estimator.fp32.onnx",
    )

    // GPT-SoVITS 下载文件列表
    val GPTSOVITS_FILES = listOf(
        "s2G488k.pth",
        "s2D488k.pth",
    )

    /**
     * 获取完整的下载 URL 列表
     *
     * @param provider TTS 提供商
     * @param baseUrl 基础 URL（HuggingFace 仓库页面链接或 hf-mirror 页面链接或 GitHub releases 链接）
     * @return 完整的文件下载 URL 列表
     */
    fun getDownloadUrls(provider: LocalTTSProvider, baseUrl: String): List<String> {
        // 如果是 GitHub releases 的下载链接，直接返回（单文件）
        if (baseUrl.contains("/releases/download/")) {
            return listOf(baseUrl)
        }

        val files = when (provider) {
            LocalTTSProvider.PIPER -> PIPER_FILES
            LocalTTSProvider.SHERPA -> SHERPA_FILES
            LocalTTSProvider.ESPEAK -> ESPEAK_FILES
            LocalTTSProvider.VOXCPM -> VOXCPM_FILES
            LocalTTSProvider.CHATTTS -> CHATTTS_FILES
            LocalTTSProvider.COSYVOICE -> COSYVOICE_FILES
            LocalTTSProvider.GPTSOVITS -> GPTSOVITS_FILES
        }

        // 提取仓库基础 URL（到 resolve/main 之前的部分）
        val repoUrl = extractRepoBaseUrl(baseUrl)

        return files.map { file ->
            "$repoUrl/resolve/main/$file"
        }
    }

    /**
     * 从给定 URL 中提取仓库基础 URL
     * 支持以下格式：
     * - 页面链接: https://hf-mirror.com/user/repo
     * - 直接链接: https://hf-mirror.com/user/repo/resolve/main/path/to/file.onnx
     * - GitHub releases: https://github.com/user/repo/releases/tag/xxx
     */
    private fun extractRepoBaseUrl(url: String): String {
        // 处理 HuggingFace / hf-mirror 的 resolve/main 链接
        val resolveIndex = url.indexOf("/resolve/main/")
        if (resolveIndex != -1) {
            return url.substring(0, resolveIndex)
        }

        // 处理 GitHub releases 的 download 链接
        val downloadIndex = url.indexOf("/releases/download/")
        if (downloadIndex != -1) {
            return url.substring(0, downloadIndex)
        }

        // 处理 GitHub releases 的 tag 页面链接
        val tagIndex = url.indexOf("/releases/tag/")
        if (tagIndex != -1) {
            return url.substring(0, tagIndex)
        }

        // 去掉末尾斜杠
        return url.trimEnd('/')
    }
}
