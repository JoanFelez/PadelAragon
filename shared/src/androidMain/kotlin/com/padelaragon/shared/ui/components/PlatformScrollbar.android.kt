package com.padelaragon.shared.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual val scrollbarGutter: Dp = 0.dp

@Composable
actual fun PlatformVerticalScrollbar(modifier: Modifier, listState: LazyListState) = Unit
