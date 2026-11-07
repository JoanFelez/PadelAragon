package com.padelaragon.shared.ui.navigation

import androidx.compose.runtime.Composable
import com.padelaragon.shared.ui.screen.ChatPlaceholderScreen
import com.padelaragon.shared.ui.screen.SettingsPlaceholderScreen

/**
 * Content of the feature tabs that are not part of the league browser. Later slices (chat,
 * settings) plug in by passing their own composables to `App(...)`; the defaults are placeholders.
 */
class AppScreens(
    val chat: @Composable () -> Unit = { ChatPlaceholderScreen() },
    val settings: @Composable () -> Unit = { SettingsPlaceholderScreen() }
)
