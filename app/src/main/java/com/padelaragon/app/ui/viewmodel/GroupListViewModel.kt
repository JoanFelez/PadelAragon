package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GroupListViewModel(
    private val groupDataSource: GroupDataSource,
    private val favoritesDataSource: FavoritesDataSource
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
                    val sortedGroups = sortGroups(groups)

                    val masc = sortedGroups.count { it.gender == Gender.MASCULINA }
                    val fem = sortedGroups.count { it.gender == Gender.FEMENINA }
                    android.util.Log.d("GroupListVM", "Groups: $masc MASCULINA, $fem FEMENINA, ${sortedGroups.size} total")
                    sortedGroups.forEach { g ->
                        android.util.Log.d("GroupListVM", "  → ${g.name} | gender=${g.gender} | category=${g.category}")
                    }

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
                                launch { runCatching { groupDataSource.prefetchGroups(favIds) } }
                            }
                            launch { runCatching { groupDataSource.prefetchAllGroups() } }
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
                            groups = sortGroups(groups),
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

    private fun sortGroups(groups: List<LeagueGroup>): List<LeagueGroup> =
        Companion.sortGroups(groups)

    fun retry() = loadGroups()

    internal companion object {
        /** Sort groups: MASCULINA before FEMENINA, then by category, then by name. */
        fun sortGroups(groups: List<LeagueGroup>): List<LeagueGroup> {
            return groups.sortedWith(
                compareBy<LeagueGroup> {
                    when (it.gender) {
                        Gender.MASCULINA -> 0
                        Gender.FEMENINA -> 1
                    }
                }.thenBy { it.category }
                    .thenBy { it.name }
            )
        }
    }
}

class GroupListViewModelFactory(
    private val groupDataSource: GroupDataSource,
    private val favoritesDataSource: FavoritesDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GroupListViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GroupListViewModel(groupDataSource, favoritesDataSource) as T
    }
}
