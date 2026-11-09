package com.padelaragon.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.padelaragon.shared.App
import com.padelaragon.shared.ui.navigation.AppScreens
import com.padelaragon.shared.ui.screen.NotificationsSection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PadelAragonApp
        val settings = app.multiLeagueContainer.notificationSettings
        setContent {
            var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) pending?.invoke()
                pending = null
            }
            val screens = AppScreens(
                settingsExtras = {
                    NotificationsSection(
                        settings = settings,
                        onChanged = { NotificationWorker.reschedule(app, settings.enabled, settings.intervalHours) },
                        ensurePermission = { proceed ->
                            if (NotificationWorker.hasPermission(app)) proceed()
                            else {
                                pending = proceed
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            )
            App(app.multiLeagueContainer, screens = screens)
        }
    }
}
