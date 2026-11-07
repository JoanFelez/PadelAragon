package com.padelaragon.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.ui.components.LoadingErrorWrapper
import com.padelaragon.app.ui.viewmodel.GroupListViewModel
import com.padelaragon.app.ui.viewmodel.GroupListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onGroupClick: (groupId: Int, groupName: String) -> Unit,
    viewModelFactory: GroupListViewModelFactory,
    viewModel: GroupListViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Liga de Aragón 2026", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                modifier = Modifier.fillMaxSize(),
            ) {
                val grouped = uiState.groups.groupBy { it.gender }
                val favoriteGroups = uiState.groups.filter { it.id in uiState.favoriteIds }
                val masculineGroups = grouped[Gender.MASCULINA].orEmpty()
                val feminineGroups = grouped[Gender.FEMENINA].orEmpty()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (favoriteGroups.isNotEmpty()) {
                        item(key = "fav_header") {
                            GenderHeader(title = "⭐ FAVORITOS")
                        }
                        items(favoriteGroups, key = { "fav_${it.id}" }) { group ->
                            GroupItem(group = group, onGroupClick = onGroupClick)
                        }
                    }

                    if (masculineGroups.isNotEmpty()) {
                        item {
                            GenderHeader(title = "MASCULINA")
                        }
                        items(masculineGroups, key = { it.id }) { group ->
                            GroupItem(group = group, onGroupClick = onGroupClick)
                        }
                    }

                    if (feminineGroups.isNotEmpty()) {
                        item {
                            GenderHeader(title = "FEMENINA")
                        }
                        items(feminineGroups, key = { it.id }) { group ->
                            GroupItem(group = group, onGroupClick = onGroupClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun GroupItem(
    group: LeagueGroup,
    onGroupClick: (groupId: Int, groupName: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGroupClick(group.id, group.name) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = group.name,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
