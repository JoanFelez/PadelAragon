package com.padelaragon.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.app.data.model.Player
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.ui.components.LoadingErrorWrapper
import com.padelaragon.app.ui.components.MatchCard
import com.padelaragon.app.ui.viewmodel.TeamViewModel
import com.padelaragon.app.ui.viewmodel.TeamViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamId: Int,
    teamName: String,
    groupId: Int,
    onBack: () -> Unit,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    viewModelFactory: TeamViewModelFactory,
    viewModel: TeamViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.teamName.ifBlank { teamName },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LoadingErrorWrapper(
                isLoading = uiState.isLoading,
                error = uiState.error,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize()
            ) {
                var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
                val tabs = listOf("Resumen", "Plantilla", "Resultados", "Próximos", "Estadísticas")
                val playedMatches = uiState.matches.filter { it.localScore != "--" }
                val pendingMatches = uiState.matches.filter { it.localScore == "--" }
                val nextMatch = pendingMatches.firstOrNull()

                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        title,
                                        color = if (selectedTabIndex == index)
                                            MaterialTheme.colorScheme.onSecondary
                                        else
                                            MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    SectionTitle(text = "Grupo")
                                    Card(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Text(
                                            text = uiState.groupName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                uiState.standing?.let { standing ->
                                    item {
                                        SectionTitle(text = "Resumen")
                                        StandingSummaryCard(standing = standing)
                                    }
                                }

                                item {
                                    SectionTitle(text = "Próximo Partido")
                                    if (nextMatch != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Jornada ${nextMatch.jornada}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                            Card(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                MatchCard(
                                                    match = nextMatch,
                                                    modifier = Modifier,
                                                    onTeamClick = { clickedTeamId, clickedTeamName ->
                                                        if (clickedTeamId != teamId) {
                                                            onTeamClick(clickedTeamId, clickedTeamName, groupId)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No hay partidos pendientes",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                uiState.teamDetail?.let { detail ->
                                    item {
                                        TeamDetailCard(detail = detail)
                                    }
                                } ?: item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No se encontró información de la plantilla",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (playedMatches.isNotEmpty()) {
                                    val sortedPlayed = playedMatches.sortedByDescending { it.jornada }
                                    items(sortedPlayed) { match ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Jornada ${match.jornada}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                            MatchCard(
                                                match = match,
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                detail = match.detailUrl?.let { uiState.matchDetails[it] },
                                                isLoadingDetail = match.detailUrl in uiState.loadingMatchDetails,
                                                onToggleDetail = match.detailUrl?.let { url -> { viewModel.loadMatchDetail(url) } },
                                                onTeamClick = { clickedTeamId, clickedTeamName ->
                                                    if (clickedTeamId != teamId) {
                                                        onTeamClick(clickedTeamId, clickedTeamName, groupId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No hay resultados todavía",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (pendingMatches.isNotEmpty()) {
                                    val sortedPending = pendingMatches.sortedBy { it.jornada }
                                    items(sortedPending) { match ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Jornada ${match.jornada}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                            MatchCard(
                                                match = match,
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                onTeamClick = { clickedTeamId, clickedTeamName ->
                                                    if (clickedTeamId != teamId) {
                                                        onTeamClick(clickedTeamId, clickedTeamName, groupId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No hay próximos partidos",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            LaunchedEffect(Unit) { viewModel.loadAllMatchDetails() }

                            if (uiState.isLoadingStats) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (uiState.playerStats.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay estadísticas disponibles",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        SectionTitle(text = "Estadísticas de Jugadores")
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // Header row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Jugador",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(3f)
                                                    )
                                                    Text(
                                                        text = "V",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(0.7f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "D",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(0.7f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "P1",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(0.5f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "P2",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(0.5f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "P3",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                        modifier = Modifier.weight(0.5f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                                                )

                                                // Player rows
                                                uiState.playerStats.forEach { stats ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = stats.name,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(3f)
                                                        )
                                                        Text(
                                                            text = stats.wins.toString(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(0.7f),
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = stats.losses.toString(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(0.7f),
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = stats.pair1Count.toString(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(0.5f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = stats.pair2Count.toString(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(0.5f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = stats.pair3Count.toString(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.weight(0.5f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun StandingSummaryCard(standing: StandingRow) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryRow(
                leftLabel = "Posición",
                leftValue = standing.position.toString(),
                emphasizeValues = true
            )
            SummaryRow(
                leftLabel = "Puntos",
                leftValue = standing.points.toString(),
                rightLabel = "Jornadas Jugadas",
                rightValue = standing.matchesPlayed.toString(),
                emphasizeValues = true
            )
            SummaryRow(
                leftLabel = "Jornadas Ganadas",
                leftValue = standing.encountersWon.toString(),
                rightLabel = "Jornadas Perdidas",
                rightValue = standing.encountersLost.toString()
            )
            SummaryRow(
                leftLabel = "Juegos Ganados",
                leftValue = standing.gamesWon.toString(),
                rightLabel = "Juegos Perdidos",
                rightValue = standing.gamesLost.toString()
            )
            SummaryRow(
                leftLabel = "Partidos Ganados",
                leftValue = standing.matchesWon.toString(),
                rightLabel = "Partidos Perdidos",
                rightValue = standing.matchesLost.toString()
            )
            SummaryRow(
                leftLabel = "Sets Ganados",
                leftValue = standing.setsWon.toString(),
                rightLabel = "Sets Perdidos",
                rightValue = standing.setsLost.toString()
            )
        }
    }
}

@Composable
private fun TeamDetailCard(detail: TeamDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show captain name
            val captainDisplay = detail.captain?.name ?: detail.captainName
            captainDisplay?.let { capName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ Capitán:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = capName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            if (detail.players.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Jugador",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(3f)
                    )
                    Text(
                        text = "Puntos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Año",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }

                detail.players.sortedByDescending { it.birthYear?.toIntOrNull() ?: 0 }.forEach { player: Player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (player.isCaptain) "⭐" else "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(3f)
                        )
                        Text(
                            text = player.points ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = player.birthYear ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (detail.players.isEmpty()) {
                Text(
                    text = "No se encontró información de la plantilla",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String? = null,
    rightValue: String? = null,
    emphasizeValues: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f),
            emphasize = emphasizeValues
        )
        if (rightLabel != null && rightValue != null) {
            StatCell(
                label = rightLabel,
                value = rightValue,
                modifier = Modifier.weight(1f),
                emphasize = emphasizeValues
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}
