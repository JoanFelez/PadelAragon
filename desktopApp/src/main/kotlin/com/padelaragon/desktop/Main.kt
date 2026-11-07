package com.padelaragon.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.padelaragon.shared.App
import com.padelaragon.shared.data.local.PlatformContext
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.ui.navigation.AppScreens
import com.padelaragon.shared.ui.screen.NotificationsSection
import java.awt.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POLL_TICK_MS = 60_000L

private object TrayIconPainter : Painter() {
    override val intrinsicSize = Size(64f, 64f)
    override fun DrawScope.onDraw() {
        drawCircle(Color(0xFF1B5E20))
        drawCircle(Color.White, radius = size.minDimension / 5f)
    }
}

fun main() {
    val container = MultiLeagueContainer.create(PlatformContext())
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { container.prewarmConnection() }

    application {
        val trayState = rememberTrayState()
        val notificationSettings = container.notificationSettings
        val screens = remember {
            AppScreens(settingsExtras = {
                NotificationsSection(settings = notificationSettings, onChanged = {})
            })
        }

        // Polls while the app runs; each pass only does work once the configured interval elapsed.
        LaunchedEffect(Unit) {
            while (true) {
                if (notificationSettings.enabled) {
                    container.resultNotifier.checkIfDue().forEach { event ->
                        trayState.sendNotification(androidx.compose.ui.window.Notification(event.title, event.message))
                    }
                }
                delay(POLL_TICK_MS)
            }
        }
        if (isTraySupported) Tray(icon = TrayIconPainter, state = trayState, tooltip = "PadelAragon")

        Window(
            onCloseRequest = ::exitApplication,
            title = "PadelAragon",
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp))
        ) {
            window.minimumSize = Dimension(480, 600)
            App(container, screens = screens, blockingFirstRunSync = true)
        }
    }
}
