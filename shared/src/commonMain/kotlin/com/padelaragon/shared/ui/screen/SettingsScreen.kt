package com.padelaragon.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.shared.appVersion
import com.padelaragon.shared.data.sync.DataSourceMode
import com.padelaragon.shared.data.sync.SyncState
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.platformName
import com.padelaragon.shared.ui.viewmodel.SettingsViewModel
import com.padelaragon.shared.ui.viewmodel.SettingsViewModelFactory
import kotlin.time.Instant

/**
 * Settings tab: data source, backend override, manual sync and about.
 *
 * [extraSections] is the plug-in point for later features (notifications toggle, "buscar
 * actualizaciones"): they are rendered between the sync card and the about card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: MultiLeagueContainer,
    extraSections: @Composable ColumnScope.() -> Unit = {}
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(container))
    val ui by vm.ui.collectAsState()
    val syncState by vm.syncState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DataSourceCard(ui.mode, vm::setMode)
            BackendCard(ui, vm::saveBackend)
            SyncCard(syncState, vm::syncNow)
            extraSections()
            AboutCard()
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DataSourceCard(selected: DataSourceMode, onSelect: (DataSourceMode) -> Unit) {
    SectionCard("Origen de los datos") {
        DataSourceMode.entries.forEach { mode ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = mode == selected, role = Role.RadioButton) {
                    onSelect(mode)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (mode) {
                            DataSourceMode.AUTO -> "Backend si está disponible; si no, la web de la federación."
                            DataSourceMode.BACKEND -> "Solo el backend de PadelAragon."
                            DataSourceMode.DEVICE -> "Consulta directa a padelfederacion.es desde el dispositivo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BackendCard(ui: SettingsViewModel.UiState, onSave: (String, String) -> Unit) {
    var url by remember(ui.backendUrl) { mutableStateOf(ui.backendUrl) }
    var key by remember(ui.appKey) { mutableStateOf(ui.appKey) }
    SectionCard("Backend") {
        Text(
            if (ui.hasEffectiveBackend) "Backend configurado." else "Sin backend: se usa la web de la federación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL del backend (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Clave de la app (opcional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Déjalos vacíos para usar los valores de la compilación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { onSave(url, key) }) { Text("Guardar y sincronizar") }
    }
}

@Composable
private fun SyncCard(state: SyncState, onSyncNow: () -> Unit) {
    SectionCard("Sincronización") {
        Text("Última sincronización: ${formatSyncTime(state.lastSyncAtMs)}", style = MaterialTheme.typography.bodyMedium)
        state.lastKind?.let { kind ->
            Text(
                listOfNotNull(kind.label, state.lastMessage).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSyncNow, enabled = !state.isSyncing) { Text("Sincronizar ahora") }
            if (state.isSyncing) CircularProgressIndicator(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun AboutCard() {
    SectionCard("Acerca de") {
        Text("PadelAragon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Versión $appVersion · $platformName", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Datos de padelfederacion.es, con caché local.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun formatSyncTime(epochMs: Long): String =
    if (epochMs <= 0L) "nunca"
    else Instant.fromEpochMilliseconds(epochMs).toString().replace('T', ' ').take(16) + " UTC"
