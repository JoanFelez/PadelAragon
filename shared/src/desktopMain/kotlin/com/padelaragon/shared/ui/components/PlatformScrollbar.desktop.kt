package com.padelaragon.shared.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual val scrollbarGutter: Dp = 12.dp

@Composable
actual fun PlatformVerticalScrollbar(modifier: Modifier, listState: LazyListState) {
    VerticalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(listState),
        style = visibleScrollbarStyle()
    )
}
