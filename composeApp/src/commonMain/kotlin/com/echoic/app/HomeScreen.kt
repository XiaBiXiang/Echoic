package com.echoic.app

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.stats.UsageStatsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    statsManager: UsageStatsManager,
) {
    val strings = LocalStrings.current
    val stats by statsManager.stats.collectAsState()
    val mostUsedCloud = stats.getMostUsedProvider()?.displayName
    val mostUsedLocal = stats.getMostUsedLocalProvider()?.displayName
    val lastUsedProvider = stats.lastUsedProvider
    val lastUsedTime = stats.lastUsedTime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        // Welcome Section
        Text(
            text = strings.welcome,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Echoic",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(32.dp))

        // Stats Cards
        Text(
            text = "📊 " + strings.totalUsage,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "📊",
                label = strings.totalUsage,
                value = stats.totalUsageCount.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "📝",
                label = strings.totalCharacters,
                value = formatNumber(stats.totalCharacters),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "☁️",
                label = strings.mostUsedCloud,
                value = mostUsedCloud ?: "-",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "💾",
                label = strings.mostUsedLocal,
                value = mostUsedLocal ?: "-",
            )
        }

        Spacer(Modifier.height(32.dp))

        // Quick Actions
        Text(
            text = "⚡ Quick Actions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = strings.startGenerating,
                onClick = { onNavigate(Screen.CLOUD_TTS) },
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = strings.manageProviders,
                onClick = { onNavigate(Screen.PROVIDERS) },
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = strings.manageLocalModels,
                onClick = { onNavigate(Screen.LOCAL_MODELS) },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Recent Usage
        if (lastUsedProvider != null && lastUsedTime != null) {
            Text(
                text = "🕐 " + strings.recentUsage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            RecentUsageItem(
                providerName = lastUsedProvider,
                timestamp = lastUsedTime,
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecentUsageItem(
    providerName: String,
    timestamp: Long,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = providerName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = dateFormat.format(Date(timestamp)),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
