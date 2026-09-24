package com.padelaragon.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.app.domain.usecase.ComputePlayerDetailStatsUseCase
import com.padelaragon.app.ui.viewmodel.TeamViewModel
import com.padelaragon.app.ui.viewmodel.TeamViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    playerName: String,
    onBack: () -> Unit,
    viewModelFactory: TeamViewModelFactory,
    viewModel: TeamViewModel = viewModel(factory = viewModelFactory)
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAllMatchDetails() }
    val stats = ComputePlayerDetailStatsUseCase()(
        state.matchDetails, state.matches.filter { it.localScore != "--" }, viewModel.teamId, playerName
    )

    Scaffold(topBar = { TopAppBar(title = { Text(playerName) }) }) { padding ->
        if (state.isLoading || state.isLoadingStats) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text("Cargando información del jugador")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.matchDetailsUnavailable) {
                    item {
                        Text(
                            "Parte del historial del jugador no está disponible.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = viewModel::retryMatchDetails) { Text("Reintentar") }
                    }
                }
                item {
                    Text("Partidos: ${stats.matchesWon} ganados · ${stats.matchesLost} perdidos",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Juegos: ${stats.gamesWon} ganados · ${stats.gamesLost} perdidos")
                }
                item { Text("Partidos en casa", style = MaterialTheme.typography.titleSmall) }
                if (stats.homeMatches.isEmpty()) item { Text("No hay partidos en casa") }
                items(stats.homeMatches) { record ->
                    Text("Jornada ${record.jornada}: ${if (record.won) "Victoria" else "Derrota"}")
                }
                item { Text("Partidos fuera", style = MaterialTheme.typography.titleSmall) }
                if (stats.awayMatches.isEmpty()) item { Text("No hay partidos fuera") }
                items(stats.awayMatches) { record ->
                    Text("Jornada ${record.jornada}: ${if (record.won) "Victoria" else "Derrota"}")
                }
                item { Button(onClick = onBack) { Text("Volver al equipo") } }
            }
        }
    }
}
