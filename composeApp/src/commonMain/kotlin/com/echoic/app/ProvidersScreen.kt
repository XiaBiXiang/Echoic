package com.echoic.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.TTSTag

@Composable
fun SectionHeader(
    icon: String,
    title: String,
    subtitle: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp),
            )
        }
    }
}

@Composable
fun ProvidersScreen(
    config: AppConfig,
    configData: AppConfigData,
    onUpdate: () -> Unit,
) {
    val strings = LocalStrings.current
    var selectedCloudTags by remember { mutableStateOf(setOf<TTSTag>()) }
    var expandedProvider by remember { mutableStateOf<TTSProvider?>(null) }

    val allCloudTags = remember { TTSProvider.entries.flatMap { it.tags }.distinct() }

    val filteredProviders = remember(selectedCloudTags) {
        if (selectedCloudTags.isEmpty()) TTSProvider.entries
        else TTSProvider.entries.filter { provider -> selectedCloudTags.all { it in provider.tags } }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Page title
        Text(
            text = strings.providers,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Scrollable content
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== Cloud Services Section =====
            SectionHeader(
                icon = "\u2601\uFE0F",
                title = strings.cloudServices,
                subtitle = strings.cloudServicesDesc,
            )

            // Tag filter bar for cloud providers
            if (allCloudTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.filterByTags,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        allCloudTags.forEach { tag ->
                            TagChip(
                                name = tag.displayName(configData.language),
                                isSelected = tag in selectedCloudTags,
                                onClick = {
                                    selectedCloudTags = if (tag in selectedCloudTags) selectedCloudTags - tag else selectedCloudTags + tag
                                },
                            )
                        }
                    }
                }
            }

            // Cloud provider cards
            filteredProviders.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    config = config,
                    configData = configData,
                    isExpanded = expandedProvider == provider,
                    onToggleExpand = {
                        expandedProvider = if (expandedProvider == provider) null else provider
                    },
                    onUpdate = onUpdate,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ===== Local Models Section =====
            SectionHeader(
                icon = "\uD83D\uDCBB",
                title = strings.localModels,
                subtitle = strings.localModelsDesc,
            )

            LocalTTSProvider.entries.forEach { provider ->
                LocalProviderCard(provider = provider, strings = strings)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProviderCard(
    provider: TTSProvider,
    config: AppConfig,
    configData: AppConfigData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: () -> Unit,
) {
    val strings = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Provider icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = provider.displayName.first().toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = provider.subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    provider.tags.take(3).forEach { tag ->
                        Text(
                            text = tag.displayName(configData.language),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Expand indicator
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Available models
                    val models = provider.availableModels
                    if (models.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.availableModels,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                models.forEach { model ->
                                    Text(
                                        text = model.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Available voices
                    val voices = provider.availableVoices
                    if (voices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.availableVoices,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                voices.forEach { voice ->
                                    Text(
                                        text = voice.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    // API Key configuration
                    ProviderConfigSection(
                        provider = provider,
                        config = config,
                        onUpdate = onUpdate,
                    )

                    // Visit website button
                    provider.websiteURL?.let { url ->
                        OutlinedButton(
                            onClick = { openUrl(url) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = strings.visitWebsite,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderConfigSection(
    provider: TTSProvider,
    config: AppConfig,
    onUpdate: () -> Unit,
) {
    val strings = LocalStrings.current
    var editKey by remember { mutableStateOf(config.getApiKey(provider) ?: "") }
    var editURL by remember { mutableStateOf(config.getBaseURL(provider)) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // API Key input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = strings.apiKey,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(70.dp),
            )
            OutlinedTextField(
                value = editKey,
                onValueChange = { editKey = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(provider.apiKeyPlaceholder, fontSize = 12.sp) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            )
            IconButton(onClick = { showKey = !showKey }, modifier = Modifier.size(28.dp)) {
                Text(if (showKey) "🙈" else "👁", fontSize = 12.sp)
            }
        }

        // Base URL input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = strings.baseUrl,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(70.dp),
            )
            OutlinedTextField(
                value = editURL,
                onValueChange = { editURL = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            )
            if (editURL != provider.defaultBaseURL) {
                IconButton(onClick = { editURL = provider.defaultBaseURL }, modifier = Modifier.size(28.dp)) {
                    Text("↺", fontSize = 12.sp)
                }
            }
        }

        // Help link
        provider.helpURL?.let { url ->
            Text(
                text = strings.getApiKey + " ↗",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { openUrl(url) }
                    .padding(vertical = 2.dp),
            )
        }

        // Save button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    config.setApiKey(provider, editKey)
                    config.setBaseURL(provider, editURL)
                    onUpdate()
                },
                modifier = Modifier.height(32.dp),
            ) {
                Text(strings.saveConfig, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LocalProviderCard(provider: LocalTTSProvider, strings: Strings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Provider header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Provider icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = provider.displayName.first().toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = provider.subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Platform support tags
            if (provider.platformSupport.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.platformSupport,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        provider.platformSupport.take(5).forEach { platform ->
                            Text(
                                text = platform.displayName,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (provider.platformSupport.size > 5) {
                            Text(
                                text = "+${provider.platformSupport.size - 5}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Download button
                provider.downloadURL?.let { url ->
                    Button(
                        onClick = { openUrl(url) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = strings.downloadModel,
                            fontSize = 12.sp,
                        )
                    }
                }

                // GitHub button
                provider.githubURL?.let { url ->
                    OutlinedButton(
                        onClick = { openUrl(url) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = strings.viewOnGitHub,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Documentation button
                provider.documentationURL?.let { url ->
                    OutlinedButton(
                        onClick = { openUrl(url) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = strings.viewDocumentation,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
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
