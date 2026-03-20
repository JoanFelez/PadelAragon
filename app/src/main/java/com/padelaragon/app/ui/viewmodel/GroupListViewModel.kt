package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.repository.LeagueRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupListViewModel : ViewModel() {
    private val repository = LeagueRepository

    data class UiState(
        val groups: List<LeagueGroup> = emptyList(),
        val favoriteIds: Set<Int> = emptySet(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            FavoritesManager.favorites.collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching { repository.getGroups() }
                .onSuccess { groups ->
                    val sortedGroups = groups.sortedWith(
                        compareBy<LeagueGroup> {
                            when (it.gender) {
                                Gender.MASCULINA -> 0
                                Gender.FEMENINA -> 1
                            }
                        }.thenBy { it.category }
                            .thenBy { it.name }
                    )

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

                    // Prefetch favorites first (so they load instantly when tapped), then all groups
                    viewModelScope.launch {
                        val favIds = FavoritesManager.favorites.value.toList()
                        if (favIds.isNotEmpty()) {
                            runCatching { repository.prefetchGroups(favIds) }
                        }
                        runCatching { repository.prefetchAllGroups() }
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

    fun retry() = loadGroups()
}
