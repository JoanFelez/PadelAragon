package com.padelaragon.shared.ui.screen

import com.padelaragon.shared.ui.components.PlatformVerticalScrollbar
import com.padelaragon.shared.ui.components.scrollbarGutter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.shared.ui.components.LoadingErrorWrapper
import com.padelaragon.shared.ui.components.MatchCard
import com.padelaragon.shared.ui.components.StandingsTable
import com.padelaragon.shared.ui.viewmodel.GroupDetailViewModel
import com.padelaragon.shared.ui.viewmodel.GroupDetailViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    groupName: String,
    onBack: () -> Unit,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    viewModelFactory: GroupDetailViewModelFactory,
    viewModel: GroupDetailViewModel = viewModel(factory = viewModelFactory)
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = groupName, color = MaterialTheme.colorScheme.onPrimaryContainer) },
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
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                            tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
        GroupDetailContent(
            groupId = groupId,
            onTeamClick = onTeamClick,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * Reusable group-detail content (tabs + standings/results) without its own
 * Scaffold/TopAppBar/back button. Used both by [GroupDetailScreen] (full-screen,
 * for mobile-style navigation) and by the desktop master-detail layout where
 * this is embedded directly in the main panel next to a persistent sidebar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailContent(
    groupId: Int,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    viewModel: GroupDetailViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Clasificación", "Resultados")
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    Box(modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    LoadingErrorWrapper(
                        isLoading = uiState.isLoadingStandings,
                        error = uiState.standingsError,
                        onRetry = viewModel::retryStandings,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        StandingsTable(
                            standings = uiState.standings,
                            modifier = Modifier.fillMaxSize(),
                            onTeamClick = { teamId, teamName ->
                                onTeamClick(teamId, teamName, groupId)
                            }
                        )
                    }
                }

                else -> {
                    ResultsTabContent(
                        jornadas = uiState.jornadas,
                        selectedJornada = uiState.selectedJornada,
                        isLoading = uiState.isLoadingResults,
                        error = uiState.resultsError,
                        onSelectJornada = viewModel::selectJornada,
                        onRetry = viewModel::retryResults,
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            if ((uiState.allMatchResults[uiState.selectedJornada] ?: emptyList()).isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sin resultados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                val matchListState = androidx.compose.foundation.lazy.rememberLazyListState()
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = matchListState,
                                        modifier = Modifier.fillMaxSize().padding(end = scrollbarGutter),
                                        contentPadding = PaddingValues(bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(uiState.allMatchResults[uiState.selectedJornada] ?: emptyList()) { match ->
                                            MatchCard(
                                                match = match,
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                detail = match.detailUrl?.let { uiState.matchDetails[it] },
                                                isLoadingDetail = match.detailUrl in uiState.loadingMatchDetails,
                                                onToggleDetail = match.detailUrl?.let { url -> { viewModel.loadMatchDetail(url) } },
                                                onTeamClick = { teamId, teamName ->
                                                    onTeamClick(teamId, teamName, groupId)
                                                }
                                            )
                                        }
                                    }
                                    PlatformVerticalScrollbar(
    modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
    listState = matchListState
)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsTabContent(
    jornadas: List<Int>,
    selectedJornada: Int?,
    isLoading: Boolean,
    error: String?,
    onSelectJornada: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = jornadas.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = selectedJornada?.let { "Jornada $it" } ?: "Selecciona jornada"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.94f).background(MaterialTheme.colorScheme.surface)
            ) {
                jornadas.forEach { jornada ->
                    DropdownMenuItem(
                        text = { Text("Jornada $jornada", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            expanded = false
                            onSelectJornada(jornada)
                        }
                    )
                }
            }
        }

        LoadingErrorWrapper(
            isLoading = isLoading,
            error = error,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            content()
        }
    }
}
