package com.padelaragon.shared.ui.screen

import com.padelaragon.shared.ui.components.PlatformVerticalScrollbar
import com.padelaragon.shared.ui.components.scrollbarGutter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.Player
import com.padelaragon.shared.data.model.StandingRow
import com.padelaragon.shared.data.model.TeamDetail
import com.padelaragon.shared.domain.usecase.AgedPlayer
import com.padelaragon.shared.domain.usecase.CoupleCombination
import com.padelaragon.shared.domain.usecase.GeneratePossibleCouplesUseCase
import com.padelaragon.shared.ui.components.LoadingErrorWrapper
import com.padelaragon.shared.ui.components.MatchCard
import com.padelaragon.shared.ui.viewmodel.TeamViewModel
import com.padelaragon.shared.ui.viewmodel.TeamViewModelFactory
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamId: Int,
    teamName: String,
    groupId: Int,
    league: League? = null,
    onBack: () -> Unit,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    onPlayerClick: (playerName: String, matchDetails: Map<String, MatchDetail>, playedMatches: List<MatchResult>) -> Unit,
    viewModelFactory: TeamViewModelFactory,
    viewModelKey: String = "team_$teamId",
    viewModel: TeamViewModel = viewModel(key = viewModelKey, factory = viewModelFactory)
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
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(20.dp).height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Actualizar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
        Box(
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
                            ScrollbarLazyColumn(
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
                            val isVeteranos = league?.id == League.VETERANOS.id
                            var selectedPlayerNames by remember(uiState.teamDetail) {
                                mutableStateOf(setOf<String>())
                            }
                            ScrollbarLazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                uiState.teamDetail?.let { detail ->
                                    item {
                                        TeamDetailCard(
                                            detail = detail,
                                            showCheckboxes = isVeteranos,
                                            selectedPlayerNames = selectedPlayerNames,
                                            onToggleSelection = { name ->
                                                selectedPlayerNames = if (name in selectedPlayerNames) {
                                                    selectedPlayerNames - name
                                                } else {
                                                    selectedPlayerNames + name
                                                }
                                            }
                                        )
                                    }
                                    if (isVeteranos) {
                                        item {
                                            CouplesGeneratorCard(
                                                players = detail.players,
                                                groupName = uiState.groupName,
                                                category = detail.category,
                                                selectedPlayerNames = selectedPlayerNames
                                            )
                                        }
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
                            ScrollbarLazyColumn(
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
                            ScrollbarLazyColumn(
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
                                ScrollbarLazyColumn(
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
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Group sub-header row (Casa / Fuera)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Spacer(modifier = Modifier.weight(STATS_JUGADOR_WEIGHT))
                                                        StatsGroupLabel(text = "Casa", weight = STATS_GROUP_WEIGHT)
                                                        Spacer(modifier = Modifier.width(STATS_GROUP_GAP))
                                                        StatsGroupLabel(text = "Fuera", weight = STATS_GROUP_WEIGHT)
                                                    }

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
                                                            modifier = Modifier.weight(STATS_JUGADOR_WEIGHT)
                                                        )
                                                        StatsColumnHeader("V")
                                                        StatsColumnHeader("D")
                                                        StatsColumnHeader("P1")
                                                        StatsColumnHeader("P2")
                                                        StatsColumnHeader("P3")
                                                        Spacer(modifier = Modifier.width(STATS_GROUP_GAP))
                                                        StatsColumnHeader("V")
                                                        StatsColumnHeader("D")
                                                        StatsColumnHeader("P1")
                                                        StatsColumnHeader("P2")
                                                        StatsColumnHeader("P3")
                                                    }

                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                                                    )

                                                    // Player rows
                                                    uiState.playerStats.forEach { stats ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    onPlayerClick(stats.name, uiState.matchDetails, playedMatches)
                                                                }
                                                                .padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = stats.name,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.weight(STATS_JUGADOR_WEIGHT)
                                                            )
                                                            StatsColumnValue(stats.home.wins.toString(), emphasized = true)
                                                            StatsColumnValue(stats.home.losses.toString(), emphasized = true)
                                                            StatsColumnValue(stats.home.pair1Count.toString())
                                                            StatsColumnValue(stats.home.pair2Count.toString())
                                                            StatsColumnValue(stats.home.pair3Count.toString())
                                                            Spacer(modifier = Modifier.width(STATS_GROUP_GAP))
                                                            StatsColumnValue(stats.away.wins.toString(), emphasized = true)
                                                            StatsColumnValue(stats.away.losses.toString(), emphasized = true)
                                                            StatsColumnValue(stats.away.pair1Count.toString())
                                                            StatsColumnValue(stats.away.pair2Count.toString())
                                                            StatsColumnValue(stats.away.pair3Count.toString())
                                                        }
                                                    }
                                                }

                                                // Single continuous red rule separating the Casa/Fuera blocks,
                                                // overlaid on top of the table so it isn't broken up by the
                                                // vertical spacing between rows. Its horizontal position is
                                                // computed with the exact same weights as the columns above,
                                                // so it lines up with the gap between the two blocks at any
                                                // card width.
                                                Row(modifier = Modifier.matchParentSize()) {
                                                    Spacer(modifier = Modifier.weight(STATS_JUGADOR_WEIGHT + STATS_GROUP_WEIGHT))
                                                    Box(
                                                        modifier = Modifier
                                                            .width(STATS_GROUP_GAP)
                                                            .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        VerticalDivider(
                                                            modifier = Modifier.fillMaxHeight(),
                                                            thickness = 1.dp,
                                                            color = Color.Red
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.weight(STATS_GROUP_WEIGHT))
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

/**
 * A [LazyColumn] with a draggable vertical scrollbar overlaid on its trailing
 * edge, so tab content (Resumen, Plantilla, Resultados, etc.) is scrollable
 * with more than just the mouse wheel.
 */
@Composable
private fun ScrollbarLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = scrollbarGutter),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content
        )
        PlatformVerticalScrollbar(
    modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
    listState = listState
)
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

/** Weight of the "Jugador" (player name) column relative to the stat columns. */
private const val STATS_JUGADOR_WEIGHT = 3f

/** Uniform weight for every V/D/P1/P2/P3 stat column, so spacing is identical across all of them. */
private const val STATS_COLUMN_WEIGHT = 0.35f

/** Combined weight of the 5 stat columns within one Casa/Fuera group. */
private val STATS_GROUP_WEIGHT = 5 * STATS_COLUMN_WEIGHT

/** Fixed-width gap between the Casa and Fuera blocks, where the red divider is drawn. */
private val STATS_GROUP_GAP = 24.dp

/** Group sub-header ("Casa" / "Fuera") spanning the 5 V/D/P1/P2/P3 columns beneath it. */
@Composable
private fun RowScope.StatsGroupLabel(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center
    )
}

/** A single column header cell ("V", "D", "P1", "P2", "P3") in the player statistics table. */
@Composable
private fun RowScope.StatsColumnHeader(text: String, weight: Float = STATS_COLUMN_WEIGHT) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center
    )
}

/** A single value cell in the player statistics table. */
@Composable
private fun RowScope.StatsColumnValue(
    text: String,
    weight: Float = STATS_COLUMN_WEIGHT,
    emphasized: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center,
        fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal
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
private fun TeamDetailCard(
    detail: TeamDetail,
    showCheckboxes: Boolean = false,
    selectedPlayerNames: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {}
) {
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
                    if (showCheckboxes) {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
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
                        if (showCheckboxes) {
                            val ageAvailable = playerAge(player) != null
                            Checkbox(
                                checked = player.name in selectedPlayerNames,
                                onCheckedChange = { if (ageAvailable) onToggleSelection(player.name) },
                                enabled = ageAvailable,
                                modifier = Modifier.width(40.dp)
                            )
                        }
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

private fun resolveGender(groupName: String?, category: String?): Gender? {
    val text = listOfNotNull(groupName, category).joinToString(" ").uppercase()
    return when {
        text.contains("FEMENINA") -> Gender.FEMENINA
        text.contains("MASCULINA") -> Gender.MASCULINA
        else -> null
    }
}

private fun playerAge(player: Player): Int? {
    val birthYear = player.birthYear?.toIntOrNull() ?: return null
    val currentYear = Year.now().value
    if (birthYear < 1900 || birthYear > currentYear) return null
    return currentYear - birthYear
}

@Composable
private fun CouplesGeneratorCard(
    players: List<Player>,
    groupName: String?,
    category: String?,
    selectedPlayerNames: Set<String>
) {
    val gender = resolveGender(groupName, category)
    val selectedAgedPlayers = remember(players, selectedPlayerNames) {
        players
            .filter { it.name in selectedPlayerNames }
            .mapNotNull { player -> playerAge(player)?.let { age -> AgedPlayer(player.name, age) } }
    }
    var result by remember(players) {
        mutableStateOf<GeneratePossibleCouplesUseCase.Result?>(null)
    }
    var currentPage by remember(players) { mutableStateOf(0) }
    val useCase = remember { GeneratePossibleCouplesUseCase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Generador de posibles parejas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (gender == null) {
                Text(
                    text = "No se pudo determinar la categoría (masculina/femenina) del grupo, " +
                        "por lo que no se pueden calcular las sumas de edad requeridas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                return@Column
            }

            val thresholds = if (gender == Gender.MASCULINA)
                GeneratePossibleCouplesUseCase.MASCULINA_THRESHOLDS
            else
                GeneratePossibleCouplesUseCase.FEMENINA_THRESHOLDS

            Text(
                text = "Categoría ${gender.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                    "Sumas mínimas requeridas: Pareja 1 ≥ ${thresholds[0]}, " +
                    "Pareja 2 ≥ ${thresholds[1]}, Pareja 3 ≥ ${thresholds[2]}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Text(
                text = "Marca en la lista de la plantilla los jugadores disponibles para el " +
                    "partido (mínimo 6). Se generarán todas las combinaciones de 3 parejas " +
                    "posibles con esos jugadores. Seleccionados: ${selectedPlayerNames.size}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Button(
                onClick = {
                    result = useCase(selectedAgedPlayers, gender)
                    currentPage = 0
                },
                enabled = selectedPlayerNames.size >= GeneratePossibleCouplesUseCase.MIN_REQUIRED_PLAYERS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar parejas")
            }

            when (val r = result) {
                null -> Unit
                GeneratePossibleCouplesUseCase.Result.NoCombinationsPossible -> {
                    Text(
                        text = "No hay ninguna combinación de parejas posible con los jugadores seleccionados.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                GeneratePossibleCouplesUseCase.Result.NotEnoughPlayersSelected -> {
                    Text(
                        text = "Selecciona al menos 6 jugadores en la plantilla (llevas ${selectedPlayerNames.size}).",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                GeneratePossibleCouplesUseCase.Result.TooManyPlayersSelected -> {
                    Text(
                        text = "Hay demasiados jugadores seleccionados para calcular todas las combinaciones. " +
                            "Selecciona 16 jugadores como máximo.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is GeneratePossibleCouplesUseCase.Result.Success -> {
                    val pageCount = (r.combinations.size + PAGE_SIZE - 1) / PAGE_SIZE
                    val page = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                    val pageItems = r.combinations
                        .drop(page * PAGE_SIZE)
                        .take(PAGE_SIZE)

                    Text(
                        text = "${r.combinations.size} combinación(es) posible(s) · " +
                            "ordenadas de más ajustada a más holgada",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    pageItems.forEachIndexed { index, combo ->
                        CoupleCombinationCard(index = page * PAGE_SIZE + index + 1, combination = combo)
                    }

                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { currentPage = (page - 1).coerceAtLeast(0) },
                                enabled = page > 0
                            ) {
                                Text("← Anterior")
                            }
                            Text(
                                text = "Página ${page + 1} de $pageCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(
                                onClick = { currentPage = (page + 1).coerceAtMost(pageCount - 1) },
                                enabled = page < pageCount - 1
                            ) {
                                Text("Siguiente →")
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val PAGE_SIZE = 10

@Composable
private fun CoupleCombinationCard(index: Int, combination: CoupleCombination) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Combinación $index",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            combination.pairs.forEach { pair ->
                Text(
                    text = "Pareja ${pair.tierIndex + 1} (suma ${pair.ageSum}, mín. ${pair.requiredSum}): " +
                        "${pair.player1.name} (${pair.player1.age}) y ${pair.player2.name} (${pair.player2.age})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
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
