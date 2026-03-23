package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import com.padelaragon.app.domain.usecase.PrefetchGroupsUseCase
import com.padelaragon.app.domain.usecase.SortGroupsUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GroupListViewModel(
    private val groupDataSource: GroupDataSource,
    private val favoritesDataSource: FavoritesDataSource,
    private val sortGroupsUseCase: SortGroupsUseCase = SortGroupsUseCase(),
    private val prefetchGroupsUseCase: PrefetchGroupsUseCase
) : ViewModel() {

    data class UiState(
        val groups: List<LeagueGroup> = emptyList(),
        val favoriteIds: Set<Int> = emptySet(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesDataSource.favorites.collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching { groupDataSource.getGroups() }
                .onSuccess { groups ->
                    val sortedGroups = sortGroupsUseCase(groups)

                    _uiState.update {
                        if (sortedGroups.isEmpty()) {
                            it.copy(
                                isLoading = false,
                                error = "No se encontraron grupos. Verifica tu conexión a internet."
                            )
                        } else {
                            it.copy(
                                groups = sortedGroups,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    viewModelScope.launch {
                        coroutineScope {
                            val favIds = favoritesDataSource.favorites.value.toList()
                            if (favIds.isNotEmpty()) {
                                launch { runCatching { prefetchGroupsUseCase.prefetchFavorites(favIds) } }
                            }
                            launch { runCatching { prefetchGroupsUseCase.prefetchAll() } }
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "No se pudieron cargar los grupos"
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            runCatching { groupDataSource.refreshGroups() }
                .onSuccess { groups ->
                    _uiState.update {
                        it.copy(
                            groups = sortGroupsUseCase(groups),
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Error al refrescar grupos")
                    }
                }

            _isRefreshing.value = false
        }
    }

    fun retry() = loadGroups()

    internal companion object {
        /** Kept for backward compatibility with existing tests. */
        fun sortGroups(groups: List<LeagueGroup>): List<LeagueGroup> =
            SortGroupsUseCase()(groups)
    }
}

class GroupListViewModelFactory(
    private val groupDataSource: GroupDataSource,
    private val favoritesDataSource: FavoritesDataSource,
    private val prefetchGroupsUseCase: PrefetchGroupsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GroupListViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GroupListViewModel(groupDataSource, favoritesDataSource, prefetchGroupsUseCase = prefetchGroupsUseCase) as T
    }
}
