package com.padelaragon.shared.ui.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.padelaragon.shared.ui.screen.ChatPlaceholderScreen

/**
 * Content of the feature tabs that are not part of the league browser. Later slices (chat)
 * plug in by passing their own composables to `App(...)`; the defaults are placeholders.
 */
class AppScreens(
    val chat: @Composable () -> Unit = { ChatPlaceholderScreen() },
    /** Null uses the built-in settings screen (sync mode, backend, about). */
    val settings: (@Composable () -> Unit)? = null,
    /** Extra settings sections (notifications toggle, update check) shown by the built-in settings screen. */
    val settingsExtras: @Composable ColumnScope.() -> Unit = {}
)
