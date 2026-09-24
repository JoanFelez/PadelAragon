package com.padelaragon.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.padelaragon.app.data.model.League

@Composable
fun LeagueChooserScreen(onLeagueSelected: (League) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("Elige una liga", style = MaterialTheme.typography.headlineSmall)
        League.entries.forEach { league ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLeagueSelected(league) }
            ) { Text(league.displayName) }
        }
    }
}
