package com.padelaragon.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.padelaragon.shared.App
import com.padelaragon.shared.data.local.PlatformContext
import com.padelaragon.shared.di.MultiLeagueContainer
import java.awt.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() {
    val container = MultiLeagueContainer.create(PlatformContext())
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { container.prewarmConnection() }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PadelAragon",
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp))
        ) {
            window.minimumSize = Dimension(480, 600)
            App(container, blockingFirstRunSync = true)
        }
    }
}
