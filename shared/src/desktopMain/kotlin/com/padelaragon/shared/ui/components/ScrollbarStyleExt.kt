package com.padelaragon.shared.ui.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * A [ScrollbarStyle] with higher-contrast colors than the library default
 * (which is a very faint black overlay and can be nearly invisible on a
 * light background). Used for every scrollable list/panel in the app so
 * scrollbars are clearly visible without needing to hover first.
 */
@Composable
fun visibleScrollbarStyle(): ScrollbarStyle {
    val base = defaultScrollbarStyle()
    return base.copy(
        unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        hoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
}
