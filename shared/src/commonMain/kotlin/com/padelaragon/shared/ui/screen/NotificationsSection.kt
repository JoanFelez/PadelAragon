package com.padelaragon.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.padelaragon.shared.domain.notification.NotificationSettings

/**
 * "Notificaciones de resultados" card for [com.padelaragon.shared.ui.navigation.AppScreens.settingsExtras].
 * [onChanged] lets the platform (re)schedule its background work; [ensurePermission] runs the
 * platform permission flow before enabling and calls `proceed` once notifications may be shown.
 */
@Composable
fun NotificationsSection(
    settings: NotificationSettings,
    onChanged: () -> Unit,
    ensurePermission: (proceed: () -> Unit) -> Unit = { it() }
) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    var interval by remember { mutableStateOf(settings.intervalHours) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notificaciones de resultados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisar de nuevos resultados y cambios en la clasificación de tus grupos favoritos.", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = enabled, onCheckedChange = { wanted ->
                    if (wanted) {
                        ensurePermission {
                            settings.enabled = true
                            enabled = true
                            onChanged()
                        }
                    } else {
                        settings.enabled = false
                        enabled = false
                        onChanged()
                    }
                })
            }
            if (enabled) {
                Text("Comprobar cada", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationSettings.INTERVAL_OPTIONS.forEach { hours ->
                        FilterChip(
                            selected = interval == hours,
                            onClick = {
                                settings.intervalHours = hours
                                interval = hours
                                onChanged()
                            },
                            label = { Text(if (hours == 1) "1 hora" else "$hours horas") }
                        )
                    }
                }
            }
        }
    }
}
