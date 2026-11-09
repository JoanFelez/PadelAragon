package com.padelaragon.shared.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** Width reserved at the trailing edge of scrollable lists for the platform scrollbar (zero on touch devices). */
expect val scrollbarGutter: Dp

/** Draggable scrollbar on desktop; renders nothing on touch platforms. */
@Composable
expect fun PlatformVerticalScrollbar(modifier: Modifier, listState: LazyListState)
