package com.echoic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.audio.AudioPlayer
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.download.DownloadState
import com.echoic.shared.engine.CloudTTSEngine
import com.echoic.shared.engine.TTSError
import com.echoic.shared.installer.InstallState
import com.echoic.shared.installer.ModelInstaller
import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.TTSTag
import com.echoic.shared.model.Voice
import kotlinx.coroutines.launch

/**
 * Represents the user's current model selection: either a cloud provider or a local provider.
 */
sealed class ModelSelection {
    data class Cloud(val provider: TTSProvider) : ModelSelection()
    data class Local(val provider: LocalTTSProvider) : ModelSelection()
}

private enum class ModelTab { CLOUD, LOCAL }

@Composable
fun CloudTtsScreen(
    config: AppConfig,
    configData: AppConfigData,
    engine: CloudTTSEngine,
    audioPlayer: AudioPlayer,
    onOpenSettings: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val downloadManager = remember { DownloadManager() }
    val localModelManager = remember { LocalModelManager() }
    val installer = remember { ModelInstaller(downloadManager, localModelManager) }
    var inputText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ModelTab.CLOUD) }
    var modelSelection by remember { mutableStateOf<ModelSelection>(ModelSelection.Cloud(TTSProvider.FISH_AUDIO)) }
    var selectedVoice by remember { mutableStateOf(Voice.FISH_DEFAULT) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioData by remember { mutableStateOf<ByteArray?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<TTSTag>()) }

    // 下载相关状态
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadingProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var installState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    val downloadState by downloadManager.downloadState.collectAsState()

    // Compute available tags from all cloud providers
    val allTags = remember { TTSProvider.entries.flatMap { it.tags }.distinct() }

    // Filter cloud providers by selected tags (AND logic)
    val filteredProviders = remember(selectedTags) {
        if (selectedTags.isEmpty()) TTSProvider.entries
        else TTSProvider.entries.filter { provider -> selectedTags.all { it in provider.tags } }
    }

    val strings = LocalStrings.current
    val charCount = inputText.length

    // Derive current provider info for config check
    val currentCloudProvider: TTSProvider? = (modelSelection as? ModelSelection.Cloud)?.provider
    val isConfigured = remember(configData, modelSelection) {
        when (val sel = modelSelection) {
            is ModelSelection.Cloud -> config.isProviderConfigured(sel.provider)
            is ModelSelection.Local -> true // local providers don't need configuration
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Text(
            text = "New Generation",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Create ultra-realistic speech from text",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Tab selector: Cloud Models / Local Models
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
        ) {
            TabButton(
                text = strings.cloudModels,
                isSelected = selectedTab == ModelTab.CLOUD,
                onClick = {
                    selectedTab = ModelTab.CLOUD
                    // Switch to first cloud provider
                    val first = filteredProviders.firstOrNull() ?: TTSProvider.FISH_AUDIO
                    modelSelection = ModelSelection.Cloud(first)
                    selectedVoice = first.availableVoices.firstOrNull() ?: Voice.FISH_DEFAULT
                    selectedTags = setOf()
                },
            )
            TabButton(
                text = strings.localModels,
                isSelected = selectedTab == ModelTab.LOCAL,
                onClick = {
                    selectedTab = ModelTab.LOCAL
                    val first = LocalTTSProvider.entries.first()
                    modelSelection = ModelSelection.Local(first)
                    selectedVoice = Voice.FISH_DEFAULT
                    selectedTags = setOf()
                },
            )
        }

        when (selectedTab) {
            ModelTab.CLOUD -> {
                // Tag filter bar
                TagFilterBar(
                    tags = allTags,
                    selectedTags = selectedTags,
                    language = configData.language,
                    onTagToggle = { tag ->
                        selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                        val current = (modelSelection as? ModelSelection.Cloud)?.provider
                        if (current != null && current !in filteredProviders) {
                            val first = filteredProviders.firstOrNull() ?: TTSProvider.FISH_AUDIO
                            modelSelection = ModelSelection.Cloud(first)
                        }
                    },
                )

                // Cloud provider selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredProviders.forEach { provider ->
                        ProviderChip(
                            name = provider.displayName,
                            isSelected = (modelSelection as? ModelSelection.Cloud)?.provider == provider,
                            onClick = {
                                modelSelection = ModelSelection.Cloud(provider)
                                selectedVoice = provider.availableVoices.firstOrNull() ?: Voice.FISH_DEFAULT
                            },
                        )
                    }
                }
            }

            ModelTab.LOCAL -> {
                // Tag filter bar for local providers
                val allLocalTags = remember { LocalTTSProvider.entries.flatMap { it.tags }.distinct() }
                TagFilterBar(
                    tags = allLocalTags,
                    selectedTags = selectedTags,
                    language = configData.language,
                    onTagToggle = { tag ->
                        selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                    },
                )

                // Local provider selector
                val filteredLocalProviders = remember(selectedTags) {
                    if (selectedTags.isEmpty()) LocalTTSProvider.entries
                    else LocalTTSProvider.entries.filter { provider -> selectedTags.all { it in provider.tags } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredLocalProviders.forEach { provider ->
                        LocalProviderChip(
                            name = provider.displayName,
                            isSelected = (modelSelection as? ModelSelection.Local)?.provider == provider,
                            isInstalled = false, // TODO: check actual install status
                            onClick = {
                                modelSelection = ModelSelection.Local(provider)
                            },
                        )
                    }
                }
            }
        }

        if (!isConfigured) {
            // Config prompt (cloud providers only)
            currentCloudProvider?.let { provider ->
                ConfigPrompt(
                    provider = provider,
                    onNavigateToProviders = { onNavigate(Screen.PROVIDERS) },
                )
            }
        } else {
            when (val sel = modelSelection) {
                is ModelSelection.Cloud -> {
                    // Voice picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Voice:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        sel.provider.availableVoices.forEach { voice ->
                            VoiceChip(
                                name = voice.displayName,
                                isSelected = voice == selectedVoice,
                                onClick = { selectedVoice = voice },
                            )
                        }
                    }

                    // Text input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text(strings.textPlaceholder) },
                        shape = RoundedCornerShape(10.dp),
                    )

                    // Character count + action
                    SynthesisActionRow(
                        charCount = charCount,
                        isSynthesizing = isSynthesizing,
                        inputText = inputText,
                        onGenerate = {
                            scope.launch {
                                isSynthesizing = true
                                errorMessage = null
                                try {
                                    audioData = engine.synthesize(
                                        inputText,
                                        sel.provider.availableModels.first(),
                                        selectedVoice,
                                    )
                                    audioData?.let { audioPlayer.play(it, AudioFormat.MP3) }
                                } catch (e: TTSError) {
                                    errorMessage = e.message
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Unknown error"
                                } finally {
                                    isSynthesizing = false
                                }
                            }
                        },
                        onCancel = { engine.cancel(); isSynthesizing = false },
                    )
                }

                is ModelSelection.Local -> {
                    // Local model info card
                    LocalModelInfoCard(
                        provider = sel.provider,
                        isInstalled = localModelManager.isModelInstalled(sel.provider),
                        installState = if (downloadingProvider == sel.provider) installState else null,
                        onDownload = {
                            downloadingProvider = sel.provider
                            installState = InstallState.Idle
                            showDownloadDialog = true
                            scope.launch {
                                installer.installModel(
                                    provider = sel.provider,
                                    onStateChange = { state ->
                                        installState = state
                                    },
                                )
                            }
                        },
                    )

                    // Text input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        placeholder = { Text(strings.textPlaceholder) },
                        shape = RoundedCornerShape(10.dp),
                    )

                    // Coming soon notice
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = strings.comingSoon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = strings.downloadFirst,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                        Text(msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                    }
                }
            }

            // Audio player
            audioData?.let {
                AudioPlayerBar(audioPlayer = audioPlayer)
            }
        }
    }

    // Download progress dialog
    if (showDownloadDialog && downloadingProvider != null) {
        val currentInstallState = installState
        when (currentInstallState) {
            is InstallState.Downloading -> {
                DownloadProgressDialog(
                    downloadState = DownloadState.Downloading(
                        progress = currentInstallState.progress,
                        speed = currentInstallState.speed,
                        sourceName = downloadingProvider?.displayName ?: "",
                        downloadedBytes = currentInstallState.downloadedBytes,
                        totalBytes = currentInstallState.totalBytes,
                    ),
                    providerName = downloadingProvider?.displayName ?: "",
                    onSwitchSource = {
                        // TODO: 实现切换下载源
                    },
                    onCancel = {
                        downloadManager.cancelDownload()
                        showDownloadDialog = false
                        downloadingProvider = null
                        installState = InstallState.Idle
                    },
                    onDismiss = {
                        showDownloadDialog = false
                        downloadingProvider = null
                        installState = InstallState.Idle
                    },
                )
            }
            is InstallState.Extracting -> {
                DownloadProgressDialog(
                    downloadState = DownloadState.Downloading(
                        progress = currentInstallState.progress,
                        speed = 0L,
                        sourceName = "解压中...",
                    ),
                    providerName = downloadingProvider?.displayName ?: "",
                    onSwitchSource = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
            is InstallState.Verifying -> {
                DownloadProgressDialog(
                    downloadState = DownloadState.Downloading(
                        progress = 1.0f,
                        speed = 0L,
                        sourceName = "验证中...",
                    ),
                    providerName = downloadingProvider?.displayName ?: "",
                    onSwitchSource = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
            is InstallState.Completed -> {
                DownloadProgressDialog(
                    downloadState = DownloadState.Completed(currentInstallState.path),
                    providerName = downloadingProvider?.displayName ?: "",
                    onSwitchSource = {},
                    onCancel = {},
                    onDismiss = {
                        showDownloadDialog = false
                        downloadingProvider = null
                        installState = InstallState.Idle
                    },
                )
            }
            is InstallState.Failed -> {
                DownloadProgressDialog(
                    downloadState = DownloadState.Failed(currentInstallState.error),
                    providerName = downloadingProvider?.displayName ?: "",
                    onSwitchSource = {},
                    onCancel = {},
                    onDismiss = {
                        showDownloadDialog = false
                        downloadingProvider = null
                        installState = InstallState.Idle
                    },
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProviderChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun LocalProviderChip(name: String, isSelected: Boolean, isInstalled: Boolean, onClick: () -> Unit) {
    val statusColor = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val strings = LocalStrings.current
        Text(
            text = if (isInstalled) strings.installed else strings.notInstalled,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else statusColor,
        )
    }
}

@Composable
private fun LocalModelInfoCard(
    provider: LocalTTSProvider,
    isInstalled: Boolean = false,
    installState: InstallState? = null,
    onDownload: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val isInstalling = installState != null &&
        installState !is InstallState.Idle &&
        installState !is InstallState.Completed &&
        installState !is InstallState.Failed

    TTSContentCard(
        title = provider.displayName,
        subtitle = provider.subtitle,
    ) {
        provider.modelSizeMB?.let { size ->
            Text(
                text = "${strings.modelSize}: $size MB",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 安装进度显示
        when (installState) {
            is InstallState.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${strings.downloading}: ${(installState.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { installState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            is InstallState.Extracting -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${strings.extracting}: ${(installState.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { installState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            is InstallState.Verifying -> {
                Text(
                    text = strings.verifying,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            is InstallState.Completed -> {
                Text(
                    text = strings.installComplete,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            is InstallState.Failed -> {
                Text(
                    text = "${strings.installFailed}: ${installState.error}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> {}
        }

        // 下载/已安装按钮
        if (isInstalled) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.modelAlreadyInstalled)
            }
        } else {
            Button(
                onClick = onDownload,
                enabled = !isInstalling,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.installing)
                } else {
                    Text(strings.downloadModel)
                }
            }
        }
    }
}

@Composable
private fun SynthesisActionRow(
    charCount: Int,
    isSynthesizing: Boolean,
    inputText: String,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$charCount / 5000 characters",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isSynthesizing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Synthesizing...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCancel) {
                    Text(strings.cancel)
                }
            }
        } else {
            Button(
                onClick = onGenerate,
                enabled = inputText.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(">> ${strings.generate}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VoiceChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TagChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ConfigPrompt(provider: TTSProvider, onNavigateToProviders: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83D\uDD11",
                    fontSize = 28.sp,
                )
            }

            // Title
            Text(
                text = strings.apiKeyRequired,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Description
            Text(
                text = strings.apiKeyRequiredDesc.format(provider.displayName),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onNavigateToProviders,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "${strings.navigateToConfig} →",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                provider.helpURL?.let { url ->
                    TextButton(onClick = { openUrl(url) }) {
                        Text(
                            text = strings.learnMore,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A reusable card layout for both cloud and local TTS provider content.
 * Provides consistent styling with title, subtitle, and content slots.
 */
@Composable
fun TTSContentCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/**
 * A reusable tag filter bar that works with any list of TTSTag.
 * Used by both cloud and local model tabs.
 */
@Composable
fun TagFilterBar(
    tags: List<TTSTag>,
    selectedTags: Set<TTSTag>,
    language: String,
    onTagToggle: (TTSTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    if (tags.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = strings.filterByTags,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                tags.forEach { tag ->
                    TagChip(
                        name = tag.displayName(language),
                        isSelected = tag in selectedTags,
                        onClick = { onTagToggle(tag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerBar(audioPlayer: AudioPlayer) {
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentTime by audioPlayer.currentTime.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = {
                if (isPlaying) audioPlayer.pause() else audioPlayer.resume()
            }) {
                Text(if (isPlaying) "||" else ">>", fontSize = 20.sp)
            }

            Text(
                text = formatTime(currentTime),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LinearProgressIndicator(
                progress = { if (duration > 0) (currentTime / duration).toFloat() else 0f },
                modifier = Modifier.weight(1f).height(4.dp),
            )

            Text(
                text = formatTime(duration),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(seconds: Double): String {
    val mins = seconds.toLong() / 60
    val secs = seconds.toLong() % 60
    return "%d:%02d".format(mins, secs)
}
