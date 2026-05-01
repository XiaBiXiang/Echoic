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
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData

@Composable
fun SettingsScreen(
    config: AppConfig,
    configData: AppConfigData,
    onUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // General section
        SettingsSection(title = "GENERAL") {
            // Appearance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Appearance", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Set your preferred interface theme.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SegmentedPicker(
                    options = listOf("Light", "Dark", "System"),
                    selected = when (configData.appearance) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        else -> "System"
                    },
                    onSelect = {
                        config.updateAppearance(it.lowercase())
                        onUpdate()
                    },
                )
            }

            // Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Language", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Primary system language.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SegmentedPicker(
                    options = listOf("English", "中文"),
                    selected = if (configData.language == "zh") "中文" else "English",
                    onSelect = {
                        config.updateLanguage(if (it == "中文") "zh" else "en")
                        onUpdate()
                    },
                )
            }
        }

        // Footer
        Text(
            text = "Echoic v1.0.0",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SegmentedPicker(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            Text(
                text = option,
                fontSize = 13.sp,
                fontWeight = if (option == selected) FontWeight.Medium else FontWeight.Normal,
                color = if (option == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (option == selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            )
        }
    }
}
