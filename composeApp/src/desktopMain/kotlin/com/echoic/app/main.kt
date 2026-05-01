package com.echoic.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.echoic.shared.config.AppConfig
import echoic.composeapp.generated.resources.Res
import echoic.composeapp.generated.resources.icon
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
fun main() {
    val config = AppConfig(AppConfig.defaultPath())

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Echoic",
            icon = painterResource(Res.drawable.icon),
        ) {
            App(config = config)
        }
    }
}
