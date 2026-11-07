package com.padelaragon.shared.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** "Nueva versión disponible" card; renders nothing when up to date or the check failed. */
@Composable
fun UpdateSection(checker: UpdateChecker) {
    var info by remember { mutableStateOf(checker.cached()) }
    LaunchedEffect(checker) { info = checker.checkIfDue() }
    val update = info ?: return
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nueva versión disponible", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Versión ${update.version}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { runCatching { uriHandler.openUri(update.url) } }) { Text("Ver versión") }
        }
    }
}
