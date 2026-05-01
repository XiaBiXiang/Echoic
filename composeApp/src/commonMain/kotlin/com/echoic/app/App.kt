package com.echoic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.echoic.shared.audio.AudioPlayer
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData
import com.echoic.shared.engine.TTSEngineFactory
import com.echoic.shared.stats.StatsStorage
import com.echoic.shared.stats.UsageStatsManager

enum class Screen { HOME, CLOUD_TTS, PROVIDERS, LOCAL_MODELS }

@Composable
fun App(config: AppConfig) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var configData by remember { mutableStateOf(config.data) }
    var showSettings by remember { mutableStateOf(false) }

    val engine = remember { TTSEngineFactory.create(config) }
    val audioPlayer = remember { AudioPlayer() }
    val statsManager = remember { UsageStatsManager(StatsStorage()) }

    LaunchedEffect(configData) {
        // Engine reads config dynamically, no need to recreate
    }

    EchoicTheme(darkTheme = configData.appearance == "dark") {
    CompositionLocalProvider(LocalStrings provides stringsFor(configData.language)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it },
                    onOpenSettings = { showSettings = true },
                )

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        Screen.HOME -> HomeScreen(
                            onNavigate = { currentScreen = it },
                            statsManager = statsManager,
                        )
                        Screen.CLOUD_TTS -> CloudTtsScreen(
                            config = config,
                            configData = configData,
                            engine = engine,
                            audioPlayer = audioPlayer,
                            onOpenSettings = { showSettings = true },
                            onNavigate = { currentScreen = it },
                        )
                        Screen.PROVIDERS -> ProvidersScreen(
                            config = config,
                            configData = configData,
                            onUpdate = { configData = config.data },
                        )
                        Screen.LOCAL_MODELS -> LocalModelsScreen()
                    }
                }
            }

            // Floating settings overlay
            SettingsOverlay(
                visible = showSettings,
                config = config,
                configData = configData,
                onUpdate = { configData = config.data },
                onDismiss = { showSettings = false },
            )
        }
    }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }
}
