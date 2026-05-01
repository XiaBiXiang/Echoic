package com.echoic.shared.download

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * 下载状态
 */
sealed class DownloadState {
    /** 空闲状态 */
    data object Idle : DownloadState()

    /** 下载中 */
    data class Downloading(
        val progress: Float,
        val speed: Long, // bytes per second
        val sourceName: String,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
    ) : DownloadState()

    /** 下载完成 */
    data class Completed(val path: String) : DownloadState()

    /** 下载失败 */
    data class Failed(val error: String) : DownloadState()
}

/**
 * 下载源
 */
data class DownloadSource(
    val name: String,
    val url: String,
    val priority: Int, // 优先级，数字越小优先级越高
    val isAvailable: Boolean = true,
    val description: String = "",
)

/**
 * 下载管理器
 * 支持多线路下载、断点续传、自动切换和速度测试
 */
class DownloadManager {
    private val httpClient: HttpClient = HttpClient(Java) {
        engine {
            // 配置超时等
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 600_000 // 10 分钟
            connectTimeoutMillis = 30_000 // 30 秒
            socketTimeoutMillis = 600_000 // 10 分钟
        }
        followRedirects = true
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentSource: DownloadSource? = null
    private var downloadJob: Job? = null
    private var isCancelled = false

    /**
     * 默认下载源列表
     */
    fun getDefaultSources(providerName: String): List<DownloadSource> {
        val modelPath = providerName.lowercase().replace(" ", "-")
        return listOf(
            DownloadSource(
                name = "hf-mirror.com",
                url = "https://hf-mirror.com/rhasspy/piper-voices/resolve/main/$modelPath",
                priority = 1,
                description = "国内镜像",
            ),
            DownloadSource(
                name = "HuggingFace",
                url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/$modelPath",
                priority = 2,
                description = "官方源",
            ),
            DownloadSource(
                name = "GitHub Releases",
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/model/$modelPath",
                priority = 3,
                description = "GitHub 发布页",
            ),
        )
    }

    /**
     * 下载模型，支持自动切换下载源
     */
    suspend fun downloadModel(
        sources: List<DownloadSource>,
        targetPath: String,
        onProgress: (DownloadState) -> Unit = {},
    ) {
        _downloadState.value = DownloadState.Idle
        onProgress(DownloadState.Idle)
        isCancelled = false

        val sortedSources = sources.filter { it.isAvailable }.sortedBy { it.priority }

        for (source in sortedSources) {
            if (isCancelled) break
            try {
                currentSource = source
                downloadFromSource(source, targetPath, onProgress)
                return // 成功则返回
            } catch (e: CancellationException) {
                // 协程取消，直接抛出
                throw e
            } catch (e: Exception) {
                // 失败则尝试下一个源
                println("Download from ${source.name} failed: ${e.message}")
                continue
            }
        }

        if (!isCancelled) {
            // 所有源都失败
            val failedState = DownloadState.Failed("所有下载源都不可用")
            _downloadState.value = failedState
            onProgress(failedState)
        }
    }

    /**
     * 从指定下载源下载，支持断点续传
     */
    private suspend fun downloadFromSource(
        source: DownloadSource,
        targetPath: String,
        onProgress: (DownloadState) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()

            // 检查已下载的字节数（用于断点续传）
            var existingBytes = 0L
            if (targetFile.exists()) {
                existingBytes = targetFile.length()
            }

            // 发起请求，支持 Range 头实现断点续传
            val response = httpClient.get(source.url) {
                header(HttpHeaders.Accept, "application/octet-stream")
                if (existingBytes > 0) {
                    header(HttpHeaders.Range, "bytes=$existingBytes-")
                }
            }

            // 检查响应状态
            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("HTTP ${response.status.value}: ${response.status.description}")
            }

            // 检查 Content-Type，确保不是 HTML 错误页面
            val contentType = response.headers[HttpHeaders.ContentType]?.lowercase() ?: ""
            if (contentType.contains("text/html")) {
                throw Exception("服务器返回了 HTML 页面而不是文件下载，请检查下载链接是否正确")
            }

            // 如果服务器不支持断点续传（返回 200 而不是 206），从头开始
            val resumeSupported = response.status == HttpStatusCode.PartialContent
            if (!resumeSupported && existingBytes > 0) {
                existingBytes = 0L
            }

            val channel = response.bodyAsChannel()
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
            val totalBytes = if (resumeSupported && existingBytes > 0) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = existingBytes
            var lastReportTime = System.currentTimeMillis()
            var lastDownloadedBytes = existingBytes

            // 写入文件
            if (resumeSupported && existingBytes > 0) {
                // 断点续传：追加写入
                RandomAccessFile(targetFile, "rw").use { raf ->
                    raf.seek(existingBytes)
                    while (!channel.isClosedForRead && !isCancelled) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            raf.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastReportTime

                            // 每 100ms 更新一次进度
                            if (timeDiff >= 100) {
                                val speed = if (timeDiff > 0) {
                                    (downloadedBytes - lastDownloadedBytes) * 1000 / timeDiff
                                } else 0L

                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes
                                } else 0f

                                val state = DownloadState.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    speed = speed,
                                    sourceName = source.name,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                )
                                _downloadState.value = state
                                onProgress(state)

                                lastReportTime = currentTime
                                lastDownloadedBytes = downloadedBytes
                            }
                        }
                    }
                }
            } else {
                // 全新下载
                FileOutputStream(targetFile, false).use { fos ->
                    val outputStream = fos.buffered()
                    while (!channel.isClosedForRead && !isCancelled) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastReportTime

                            // 每 100ms 更新一次进度
                            if (timeDiff >= 100) {
                                val speed = if (timeDiff > 0) {
                                    (downloadedBytes - lastDownloadedBytes) * 1000 / timeDiff
                                } else 0L

                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes
                                } else 0f

                                val state = DownloadState.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    speed = speed,
                                    sourceName = source.name,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                )
                                _downloadState.value = state
                                onProgress(state)

                                lastReportTime = currentTime
                                lastDownloadedBytes = downloadedBytes
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            if (isCancelled) {
                // 取消时不删除文件，支持续传
                val cancelledState = DownloadState.Failed("下载已取消")
                _downloadState.value = cancelledState
                onProgress(cancelledState)
                return@withContext
            }

            // 下载完成
            val completedState = DownloadState.Completed(targetPath)
            _downloadState.value = completedState
            onProgress(completedState)
        }
    }

    /**
     * 下载单个文件到指定路径
     */
    suspend fun downloadFile(
        url: String,
        destination: File,
        sourceName: String = "Direct",
        onProgress: (DownloadState) -> Unit = {},
    ) {
        _downloadState.value = DownloadState.Idle
        isCancelled = false

        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()

            var existingBytes = 0L
            if (destination.exists()) {
                existingBytes = destination.length()
            }

            val response = httpClient.get(url) {
                header(HttpHeaders.Accept, "application/octet-stream")
                if (existingBytes > 0) {
                    header(HttpHeaders.Range, "bytes=$existingBytes-")
                }
            }

            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("HTTP ${response.status.value}: ${response.status.description}")
            }

            // 检查 Content-Type，确保不是 HTML 错误页面
            val contentType = response.headers[HttpHeaders.ContentType]?.lowercase() ?: ""
            if (contentType.contains("text/html")) {
                throw Exception("服务器返回了 HTML 页面而不是文件下载，请检查下载链接是否正确")
            }

            val resumeSupported = response.status == HttpStatusCode.PartialContent
            if (!resumeSupported && existingBytes > 0) {
                existingBytes = 0L
            }

            val channel = response.bodyAsChannel()
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
            val totalBytes = if (resumeSupported && existingBytes > 0) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = existingBytes
            var lastReportTime = System.currentTimeMillis()
            var lastDownloadedBytes = existingBytes

            if (resumeSupported && existingBytes > 0) {
                RandomAccessFile(destination, "rw").use { raf ->
                    raf.seek(existingBytes)
                    while (!channel.isClosedForRead && !isCancelled) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            raf.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastReportTime
                            if (timeDiff >= 100) {
                                val speed = if (timeDiff > 0) {
                                    (downloadedBytes - lastDownloadedBytes) * 1000 / timeDiff
                                } else 0L
                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes
                                } else 0f
                                val state = DownloadState.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    speed = speed,
                                    sourceName = sourceName,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                )
                                _downloadState.value = state
                                onProgress(state)
                                lastReportTime = currentTime
                                lastDownloadedBytes = downloadedBytes
                            }
                        }
                    }
                }
            } else {
                FileOutputStream(destination, false).use { fos ->
                    val outputStream = fos.buffered()
                    while (!channel.isClosedForRead && !isCancelled) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastReportTime
                            if (timeDiff >= 100) {
                                val speed = if (timeDiff > 0) {
                                    (downloadedBytes - lastDownloadedBytes) * 1000 / timeDiff
                                } else 0L
                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes
                                } else 0f
                                val state = DownloadState.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    speed = speed,
                                    sourceName = sourceName,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                )
                                _downloadState.value = state
                                onProgress(state)
                                lastReportTime = currentTime
                                lastDownloadedBytes = downloadedBytes
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            if (isCancelled) {
                val cancelledState = DownloadState.Failed("下载已取消")
                _downloadState.value = cancelledState
                onProgress(cancelledState)
                return@withContext
            }

            val completedState = DownloadState.Completed(destination.absolutePath)
            _downloadState.value = completedState
            onProgress(completedState)
        }
    }

    /**
     * 测试下载源速度
     * 返回下载速度（bytes/s），如果不可用则返回 -1
     */
    suspend fun testSourceSpeed(url: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val response = httpClient.get(url) {
                    header(HttpHeaders.Range, "bytes=0-1023") // 只下载 1KB 测试
                }
                val endTime = System.currentTimeMillis()

                if (response.status.isSuccess() || response.status == HttpStatusCode.PartialContent) {
                    val bytes = response.bodyAsChannel().readAvailable(ByteArray(1024))
                    val timeMs = (endTime - startTime).coerceAtLeast(1)
                    bytes * 1000L / timeMs // bytes per second
                } else {
                    -1L
                }
            } catch (e: Exception) {
                -1L
            }
        }
    }

    /**
     * 自动选择最佳下载源
     * 测试所有源的速度，返回最快的
     */
    suspend fun selectBestSource(sources: List<DownloadSource>): DownloadSource? {
        val availableSources = sources.filter { it.isAvailable }
        if (availableSources.isEmpty()) return null

        // 并行测试所有源的速度
        val speeds = availableSources.associateWith { source ->
            testSourceSpeed(source.url)
        }

        // 选择速度最快的源
        return speeds.entries
            .filter { it.value > 0 }
            .maxByOrNull { it.value }
            ?.key
            ?: availableSources.firstOrNull()
    }

    /**
     * 切换下载源
     */
    suspend fun switchSource(
        source: DownloadSource,
        targetPath: String,
        onProgress: (DownloadState) -> Unit = {},
    ) {
        currentSource = source
        downloadFromSource(source, targetPath, onProgress)
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        isCancelled = true
        downloadJob?.cancel()
        _downloadState.value = DownloadState.Idle
    }

    /**
     * 重置下载状态
     */
    fun resetState() {
        isCancelled = false
        _downloadState.value = DownloadState.Idle
    }

    /**
     * 获取当前下载源
     */
    fun getCurrentSource(): DownloadSource? = currentSource

    /**
     * 下载多个文件到指定目录
     * 逐个下载，整体进度按已完成文件数 + 当前文件进度综合计算
     *
     * @param urls 待下载的文件 URL 列表
     * @param targetDir 目标目录
     * @param onProgress 进度回调
     */
    suspend fun downloadMultipleFiles(
        urls: List<String>,
        targetDir: String,
        onProgress: (DownloadState) -> Unit,
    ) {
        _downloadState.value = DownloadState.Idle
        isCancelled = false

        if (urls.isEmpty()) {
            val failedState = DownloadState.Failed("没有可下载的文件")
            _downloadState.value = failedState
            onProgress(failedState)
            return
        }

        val totalFiles = urls.size
        var completedFiles = 0
        val targetDirectory = java.io.File(targetDir)
        targetDirectory.mkdirs()

        for (url in urls) {
            if (isCancelled) break

            val fileName = url.substringAfterLast("/")
            val targetFile = java.io.File(targetDirectory, fileName)

            try {
                downloadFile(url, targetFile, sourceName = fileName) { state ->
                    when (state) {
                        is DownloadState.Downloading -> {
                            val overallProgress = (completedFiles + state.progress) / totalFiles
                            onProgress(
                                DownloadState.Downloading(
                                    progress = overallProgress,
                                    speed = state.speed,
                                    sourceName = state.sourceName,
                                    downloadedBytes = state.downloadedBytes,
                                    totalBytes = state.totalBytes,
                                )
                            )
                        }
                        is DownloadState.Completed -> {
                            completedFiles++
                            if (completedFiles == totalFiles) {
                                val completedState = DownloadState.Completed(targetDir)
                                _downloadState.value = completedState
                                onProgress(completedState)
                            }
                        }
                        is DownloadState.Failed -> {
                            onProgress(state)
                            return@downloadFile
                        }
                        else -> onProgress(state)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failedState = DownloadState.Failed("下载 $fileName 失败: ${e.message}")
                _downloadState.value = failedState
                onProgress(failedState)
                return
            }
        }

        if (!isCancelled && completedFiles == totalFiles) {
            _downloadState.value = DownloadState.Completed(targetDir)
        }
    }

    /**
     * 关闭 HttpClient
     */
    fun close() {
        httpClient.close()
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}
