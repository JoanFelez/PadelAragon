package com.padelaragon.shared.ui.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * Content of the feature tabs that are not part of the league browser. Callers can replace
 * them by passing their own composables to `App(...)`.
 */
class AppScreens(
    /** Null uses the built-in chat screen. */
    val chat: (@Composable () -> Unit)? = null,
    /** Null uses the built-in settings screen (sync mode, backend, about). */
    val settings: (@Composable () -> Unit)? = null,
    /** Extra settings sections (notifications toggle, update check) shown by the built-in settings screen. */
    val settingsExtras: @Composable ColumnScope.() -> Unit = {}
)
