package com.padelaragon.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.padelaragon.shared.appVersion
import com.padelaragon.shared.platformName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabScaffold(title: String, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            content()
        }
    }
}

/** Shown until the chat feature replaces it through [com.padelaragon.shared.ui.navigation.AppScreens.chat]. */
@Composable
fun ChatPlaceholderScreen() = TabScaffold("Chat") {
    Text("Próximamente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "Pregunta sobre el reglamento y la liga con el asistente de PadelAragon.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shown until the settings feature replaces it through [com.padelaragon.shared.ui.navigation.AppScreens.settings]. */
@Composable
fun SettingsPlaceholderScreen() = TabScaffold("Ajustes") {
    Text("PadelAragon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Versión $appVersion · $platformName", style = MaterialTheme.typography.bodyMedium)
    Text(
        "Origen de los datos: padelfederacion.es (consulta directa desde el dispositivo, con caché local).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
