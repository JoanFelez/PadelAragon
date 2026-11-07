package com.padelaragon.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.ui.components.LoadingErrorWrapper
import com.padelaragon.shared.ui.components.PlatformVerticalScrollbar
import com.padelaragon.shared.ui.components.scrollbarGutter
import com.padelaragon.shared.ui.viewmodel.GroupDetailViewModel
import com.padelaragon.shared.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.shared.ui.viewmodel.GroupListViewModel
import com.padelaragon.shared.ui.viewmodel.GroupListViewModelFactory
import kotlinx.coroutines.launch

/**
 * Categories of one league. On wide windows it is a master-detail layout (group list next to the
 * selected group's standings/results); on compact windows the list and the detail are separate
 * pages and the back button returns from the detail to the list first.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun GroupsScreen(
    title: String,
    initialGroupId: Int?,
    onBackToLeagues: () -> Unit,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    listViewModelFactory: GroupListViewModelFactory,
    groupDetailViewModelFactory: (groupId: Int, groupName: String) -> GroupDetailViewModelFactory,
    viewModelKey: String,
    listViewModel: GroupListViewModel = viewModel(key = viewModelKey, factory = listViewModelFactory)
) {
    val uiState by listViewModel.uiState.collectAsState()
    val isRefreshing by listViewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable { mutableStateOf(initialGroupId) }
    var detailRefresh: (() -> Unit)? by remember { mutableStateOf(null) }

    val navigator = rememberListDetailPaneScaffoldNavigator<Int>(
        initialDestinationHistory = buildList {
            add(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))
            if (initialGroupId != null) {
                add(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, initialGroupId))
            }
        }
    )
    val dualPane = navigator.scaffoldDirective.maxHorizontalPartitions > 1
    val detailOnly = !dualPane &&
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val selected = uiState.groups.firstOrNull { it.id == selectedId }

    // With two panes there is always room for a detail: default to a favourite, else the first group.
    LaunchedEffect(uiState.groups, uiState.favoriteIds, dualPane) {
        if (dualPane && selectedId == null && uiState.groups.isNotEmpty()) {
            selectedId = (uiState.groups.firstOrNull { it.id in uiState.favoriteIds } ?: uiState.groups.first()).id
        }
    }

    BackHandler(enabled = detailOnly) { scope.launch { navigator.navigateBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (detailOnly) selected?.name ?: title else title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (detailOnly) scope.launch { navigator.navigateBack() } else onBackToLeagues()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (detailOnly) "Volver a categorías" else "Volver a ligas"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            listViewModel.refresh()
                            detailRefresh?.invoke()
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(20.dp).height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            listPane = {
                AnimatedPane(modifier = Modifier.preferredWidth(ListPaneWidth)) {
                    GroupListPane(
                        listViewModel = listViewModel,
                        selectedId = selectedId.takeIf { dualPane || detailOnly },
                        onGroupClick = { group ->
                            selectedId = group.id
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, group.id) }
                        }
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    Row(Modifier.fillMaxSize()) {
                        if (dualPane) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        val currentGroup = selected
                        if (currentGroup == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Selecciona una categoría",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            key(currentGroup.id) {
                                GroupDetailPanel(
                                    groupId = currentGroup.id,
                                    groupName = currentGroup.name,
                                    onTeamClick = onTeamClick,
                                    viewModelFactory = groupDetailViewModelFactory(currentGroup.id, currentGroup.name),
                                    onRefreshAvailable = { refreshFn -> detailRefresh = refreshFn }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

private val ListPaneWidth = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupListPane(
    listViewModel: GroupListViewModel,
    selectedId: Int?,
    onGroupClick: (LeagueGroup) -> Unit
) {
    val uiState by listViewModel.uiState.collectAsState()
    val isRefreshing by listViewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = listViewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LoadingErrorWrapper(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onRetry = listViewModel::retry,
            modifier = Modifier.fillMaxSize()
        ) {
            val grouped = uiState.groups.groupBy { it.gender }
            val favoriteGroups = uiState.groups.filter { it.id in uiState.favoriteIds }
            val masculineGroups = grouped[Gender.MASCULINA].orEmpty()
            val feminineGroups = grouped[Gender.FEMENINA].orEmpty()
            val listState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = scrollbarGutter),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (favoriteGroups.isNotEmpty()) {
                        item(key = "fav_header") { SectionHeader(title = "⭐ FAVORITOS") }
                        items(favoriteGroups, key = { "fav_${it.id}" }) { group ->
                            GroupItem(group, group.id == selectedId) { onGroupClick(group) }
                        }
                    }
                    if (masculineGroups.isNotEmpty()) {
                        item { SectionHeader(title = "MASCULINA") }
                        items(masculineGroups, key = { it.id }) { group ->
                            GroupItem(group, group.id == selectedId) { onGroupClick(group) }
                        }
                    }
                    if (feminineGroups.isNotEmpty()) {
                        item { SectionHeader(title = "FEMENINA") }
                        items(feminineGroups, key = { it.id }) { group ->
                            GroupItem(group, group.id == selectedId) { onGroupClick(group) }
                        }
                    }
                }
                PlatformVerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    listState = listState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailPanel(
    groupId: Int,
    groupName: String,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    viewModelFactory: GroupDetailViewModelFactory,
    onRefreshAvailable: (() -> Unit) -> Unit = {},
    viewModel: GroupDetailViewModel = viewModel(
        key = "group_detail_$groupId",
        factory = viewModelFactory
    )
) {
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(viewModel) {
        onRefreshAvailable(viewModel::refresh)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        GroupDetailContent(
            groupId = groupId,
            onTeamClick = onTeamClick,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun GroupItem(group: LeagueGroup, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = group.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
