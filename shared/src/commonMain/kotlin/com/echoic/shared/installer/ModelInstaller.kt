package com.echoic.shared.installer

import com.echoic.shared.download.DownloadConfig
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.download.DownloadSource
import com.echoic.shared.download.DownloadState
import com.echoic.shared.model.DownloadMirror
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/**
 * 模型安装状态
 */
sealed class InstallState {
    /** 空闲状态 */
    data object Idle : InstallState()

    /** 下载中 */
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speed: Long = 0L,
    ) : InstallState()

    /** 解压中 */
    data class Extracting(val progress: Float) : InstallState()

    /** 验证中 */
    data object Verifying : InstallState()

    /** 安装完成 */
    data class Completed(val path: String) : InstallState()

    /** 安装失败 */
    data class Failed(val error: String, val canRetry: Boolean = true) : InstallState()
}

/**
 * 模型安装管理器
 * 提供一键安装、重新安装、选择下载源安装等功能
 */
class ModelInstaller(
    private val downloadManager: DownloadManager,
    private val localModelManager: LocalModelManager,
) {
    /**
     * 一键安装模型
     * 1. 检查是否已安装
     * 2. 选择下载源（默认使用优先级最高的）
     * 3. 下载模型文件
     * 4. 解压模型文件（如果需要）
     * 5. 验证模型文件完整性
     * 6. 更新安装状态
     */
    suspend fun installModel(
        provider: LocalTTSProvider,
        selectedSource: DownloadSource? = null,
        onStateChange: (InstallState) -> Unit,
    ) {
        // 1. 检查是否已安装
        if (isInstalled(provider)) {
            onStateChange(InstallState.Completed(getInstallPath(provider)))
            return
        }

        // 2. 确定下载源列表
        val sources = if (selectedSource != null) {
            listOf(selectedSource)
        } else {
            getDownloadSources(provider)
        }

        if (sources.isEmpty()) {
            onStateChange(InstallState.Failed("没有可用的下载源", canRetry = false))
            return
        }

        // 3. 下载模型文件
        val installDir = getInstallPath(provider)
        val downloadDir = getDownloadTempPath(provider)

        // 获取下载源 URL
        val primarySource = sources.firstOrNull()
        val primaryUrl = primarySource?.url
            ?: provider.downloadMirrors.firstOrNull { it.isDefault }?.url
            ?: provider.downloadMirrors.firstOrNull()?.url

        if (primaryUrl == null) {
            onStateChange(InstallState.Failed("没有可用的下载 URL", canRetry = false))
            return
        }

        // 使用 DownloadConfig 获取需要下载的文件列表
        val downloadUrls = DownloadConfig.getDownloadUrls(provider, primaryUrl)

        if (downloadUrls.isEmpty()) {
            onStateChange(InstallState.Failed("该模型没有可下载的文件配置", canRetry = false))
            return
        }

        var downloadSucceeded = false

        downloadManager.downloadMultipleFiles(
            urls = downloadUrls,
            targetDir = downloadDir,
            onProgress = { state ->
                when (state) {
                    is DownloadState.Downloading -> {
                        onStateChange(
                            InstallState.Downloading(
                                progress = state.progress,
                                downloadedBytes = state.downloadedBytes,
                                totalBytes = state.totalBytes,
                                speed = state.speed,
                            )
                        )
                    }
                    is DownloadState.Completed -> {
                        downloadSucceeded = true
                    }
                    is DownloadState.Failed -> {
                        onStateChange(InstallState.Failed(state.error, canRetry = true))
                    }
                    else -> {}
                }
            },
        )

        if (!downloadSucceeded) {
            return
        }

        // 4. 处理下载的文件
        val downloadDirFile = File(downloadDir)
        val downloadedFiles = downloadDirFile.listFiles() ?: emptyArray()
        val installDirFile = File(installDir)
        installDirFile.mkdirs()

        // 检查是否有需要解压的归档文件
        val archiveFile = downloadedFiles.find { file ->
            file.name.endsWith(".tar.gz") ||
                file.name.endsWith(".zip") ||
                file.name.endsWith(".gz")
        }

        val finalPath = if (archiveFile != null) {
            onStateChange(InstallState.Extracting(0f))
            try {
                extractModel(archiveFile.absolutePath, installDirFile.absolutePath) { progress ->
                    onStateChange(InstallState.Extracting(progress))
                }
                downloadedFiles.filter { it != archiveFile }.forEach { file ->
                    file.copyTo(File(installDirFile, file.name), overwrite = true)
                }
                installDirFile.absolutePath
            } catch (e: Exception) {
                onStateChange(InstallState.Failed("解压失败: ${e.message ?: "未知错误"}", canRetry = true))
                return
            } finally {
                try { downloadDirFile.deleteRecursively() } catch (_: Exception) {}
            }
        } else {
            try {
                downloadedFiles.forEach { file ->
                    val targetFile = File(installDirFile, file.name)
                    file.copyTo(targetFile, overwrite = true)
                    file.delete()
                }
                installDirFile.absolutePath
            } catch (e: Exception) {
                onStateChange(InstallState.Failed("移动文件失败: ${e.message ?: "未知错误"}", canRetry = true))
                return
            }
        }

        // 5. 验证并标记安装完成
        verifyAndMarkInstalled(provider, finalPath, onStateChange)
    }

    /**
     * 验证并标记安装完成
     */
    private suspend fun verifyAndMarkInstalled(
        provider: LocalTTSProvider,
        finalPath: String,
        onStateChange: (InstallState) -> Unit,
    ) {
        onStateChange(InstallState.Verifying)
        try {
            verifyModel(finalPath)
        } catch (e: Exception) {
            onStateChange(InstallState.Failed("验证失败: ${e.message ?: "未知错误"}", canRetry = true))
            return
        }

        val installedSize = calculateDirectorySize(File(finalPath))
        localModelManager.markAsInstalled(provider, installedSize)
        onStateChange(InstallState.Completed(finalPath))
    }

    /**
     * 重新安装模型
     * 先卸载已有模型，再执行完整安装流程
     */
    suspend fun reinstallModel(
        provider: LocalTTSProvider,
        onStateChange: (InstallState) -> Unit,
    ) {
        // 先卸载
        if (isInstalled(provider)) {
            localModelManager.uninstallModel(provider)
        }
        // 再安装
        installModel(provider, onStateChange = onStateChange)
    }

    /**
     * 选择其他线路安装
     * 使用指定的下载源进行安装
     */
    suspend fun installWithSource(
        provider: LocalTTSProvider,
        source: DownloadSource,
        onStateChange: (InstallState) -> Unit,
    ) {
        // 如果已安装，先卸载
        if (isInstalled(provider)) {
            localModelManager.uninstallModel(provider)
        }
        installModel(provider, selectedSource = source, onStateChange = onStateChange)
    }

    /**
     * 检查是否已安装
     */
    fun isInstalled(provider: LocalTTSProvider): Boolean {
        // 检查文件系统中是否存在模型目录
        val installDir = File(getInstallPath(provider))
        if (!installDir.exists() || !installDir.isDirectory) return false

        // 检查目录中是否有文件
        val files = installDir.listFiles()
        return files != null && files.isNotEmpty()
    }

    /**
     * 获取安装路径
     */
    fun getInstallPath(provider: LocalTTSProvider): String {
        val home = System.getProperty("user.home")
        return "$home/.echoic/models/${provider.name.lowercase()}"
    }

    /**
     * 获取下载临时目录
     */
    private fun getDownloadTempPath(provider: LocalTTSProvider): String {
        val home = System.getProperty("user.home")
        return "$home/.echoic/downloads/${provider.name.lowercase()}"
    }

    /**
     * 获取可用的下载源列表
     * 优先使用 provider 自带的 downloadMirrors，如果没有则使用 DownloadManager 的默认源
     */
    fun getDownloadSources(provider: LocalTTSProvider): List<DownloadSource> {
        val mirrors = provider.downloadMirrors
        return if (mirrors.isNotEmpty()) {
            mirrors.mapIndexed { index, mirror ->
                DownloadSource(
                    name = mirror.name,
                    url = mirror.url,
                    priority = if (mirror.isDefault) 0 else index + 1,
                    description = if (mirror.isDefault) "推荐" else "",
                )
            }.sortedBy { it.priority }
        } else {
            downloadManager.getDefaultSources(provider.displayName)
        }
    }

    /**
     * 解压模型文件
     * 支持 .tar.gz、.zip 和 .gz 格式
     */
    private suspend fun extractModel(
        archivePath: String,
        extractDir: String,
        onProgress: (Float) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val archiveFile = File(archivePath)
            val destDir = File(extractDir)
            destDir.mkdirs()

            when {
                archivePath.endsWith(".tar.gz") || archivePath.endsWith(".tgz") -> {
                    extractTarGz(archiveFile, destDir, onProgress)
                }
                archivePath.endsWith(".zip") -> {
                    extractZip(archiveFile, destDir, onProgress)
                }
                archivePath.endsWith(".gz") -> {
                    extractGz(archiveFile, destDir, onProgress)
                }
                else -> {
                    throw Exception("不支持的压缩格式: ${archiveFile.name}")
                }
            }
        }
    }

    /**
     * 解压 .tar.gz 文件
     */
    private fun extractTarGz(
        archiveFile: File,
        destDir: File,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        GZIPInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gzis ->
            // 简化的 tar 解析（适用于单文件模型）
            val buffer = ByteArray(8192)
            val headerBuffer = ByteArray(512)

            while (true) {
                // 读取 tar header
                val headerRead = readFully(gzis, headerBuffer)
                if (headerRead < 512) break

                // 检查是否是空块（tar 结束标志）
                if (headerBuffer.all { it == 0.toByte() }) break

                // 解析文件名（字节 0-99）
                val nameBytes = headerBuffer.copyOfRange(0, 100)
                val name = String(nameBytes, Charsets.US_ASCII).trim('\u0000')

                // 解析文件大小（字节 124-135，八进制）
                val sizeBytes = headerBuffer.copyOfRange(124, 136)
                val sizeStr = String(sizeBytes, Charsets.US_ASCII).trim('\u0000', ' ')
                val fileSize = sizeStr.toLongOrNull(8) ?: 0L

                if (name.isEmpty() || name == "./" || name == ".") {
                    // 跳过目录条目
                    continue
                }

                // 处理文件
                val outputFile = File(destDir, name.removePrefix("/"))
                if (name.endsWith("/")) {
                    // 目录
                    outputFile.mkdirs()
                } else {
                    // 文件
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fos ->
                        val outBuffer = fos.buffered()
                        var remaining = fileSize
                        while (remaining > 0) {
                            val toRead = minOf(remaining.toLong(), buffer.size.toLong()).toInt()
                            val read = gzis.read(buffer, 0, toRead)
                            if (read == -1) break
                            outBuffer.write(buffer, 0, read)
                            remaining -= read
                            bytesRead += read

                            if (totalSize > 0) {
                                onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                            }
                        }
                        outBuffer.flush()
                    }
                }

                // 跳过 tar 块对齐填充
                val padding = (512 - (fileSize % 512)).toInt()
                if (padding < 512) {
                    var skipRemaining = padding.toLong()
                    while (skipRemaining > 0) {
                        val skipped = gzis.skip(skipRemaining)
                        if (skipped <= 0) break
                        skipRemaining -= skipped
                    }
                }

                bytesRead += 512 // header
                if (totalSize > 0) {
                    onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                }
            }
        }

        onProgress(1.0f)
    }

    /**
     * 从 InputStream 读取指定数量的字节
     */
    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) return offset
            offset += read
        }
        return offset
    }

    /**
     * 解压 .zip 文件
     */
    private fun extractZip(
        archiveFile: File,
        destDir: File,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
            val buffer = ByteArray(8192)
            var entry = zis.nextEntry

            while (entry != null) {
                val outputFile = File(destDir, entry.name)

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fos ->
                        val outBuffer = fos.buffered()
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            outBuffer.write(buffer, 0, read)
                            bytesRead += read
                            if (totalSize > 0) {
                                onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                            }
                        }
                        outBuffer.flush()
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        onProgress(1.0f)
    }

    /**
     * 解压 .gz 文件（单文件压缩）
     */
    private fun extractGz(
        archiveFile: File,
        destDir: File,
        onProgress: (Float) -> Unit,
    ) {
        val totalSize = archiveFile.length()
        var bytesRead = 0L

        val outputName = archiveFile.name.removeSuffix(".gz")
        val outputFile = File(destDir, outputName)

        GZIPInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gzis ->
            FileOutputStream(outputFile).use { fos ->
                val outBuffer = fos.buffered()
                val buffer = ByteArray(8192)
                var read: Int
                while (gzis.read(buffer).also { read = it } != -1) {
                    outBuffer.write(buffer, 0, read)
                    bytesRead += read
                    if (totalSize > 0) {
                        onProgress((bytesRead.toFloat() / totalSize).coerceIn(0f, 1f))
                    }
                }
                outBuffer.flush()
            }
        }

        onProgress(1.0f)
    }

    /**
     * 验证模型文件完整性
     * 检查文件/目录是否存在且大小合理
     */
    private suspend fun verifyModel(modelPath: String) {
        withContext(Dispatchers.IO) {
            val path = File(modelPath)

            if (!path.exists()) {
                throw Exception("模型文件不存在: $modelPath")
            }

            if (path.isDirectory) {
                // 检查目录中是否有文件
                val files = path.listFiles()
                if (files == null || files.isEmpty()) {
                    throw Exception("模型目录为空: $modelPath")
                }

                // 检查总大小是否合理（至少 1KB）
                val totalSize = calculateDirectorySize(path)
                if (totalSize < 1024) {
                    throw Exception("模型文件过小 (${totalSize} bytes)，可能下载不完整")
                }
            } else {
                // 单文件检查
                if (path.length() < 1024) {
                    throw Exception("模型文件过小 (${path.length()} bytes)，可能下载不完整")
                }
            }
        }
    }

    /**
     * 计算目录总大小
     */
    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        if (directory.isFile) return directory.length()

        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}
