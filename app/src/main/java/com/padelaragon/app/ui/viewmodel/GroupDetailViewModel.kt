package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.repository.LeagueRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: Int,
    private val groupName: String
) : ViewModel() {
    private val repository = LeagueRepository

    data class UiState(
        val groupName: String = "",
        val standings: List<StandingRow> = emptyList(),
        val allMatchResults: Map<Int, List<MatchResult>> = emptyMap(),
        val jornadas: List<Int> = emptyList(),
        val selectedJornada: Int? = null,
        val isLoadingStandings: Boolean = true,
        val isLoadingResults: Boolean = true,
        val standingsError: String? = null,
        val resultsError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(groupName = groupName))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val currentResults: StateFlow<List<MatchResult>> = uiState
        .map { state -> state.selectedJornada?.let { state.allMatchResults[it] } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val isFavorite: StateFlow<Boolean> = FavoritesManager.favorites
        .map { favorites -> groupId in favorites }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FavoritesManager.isFavorite(groupId)
        )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingStandings = true,
                    isLoadingResults = true,
                    standingsError = null,
                    resultsError = null
                )
            }

            coroutineScope {
                val standingsDeferred = async { runCatching { repository.getStandings(groupId) } }
                val resultsDeferred = async { runCatching { repository.getAllMatchResults(groupId) } }

                standingsDeferred.await()
                    .onSuccess { standings ->
                        _uiState.update { it.copy(standings = standings, isLoadingStandings = false) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingStandings = false,
                                standingsError = throwable.message ?: "Error al cargar clasificacion"
                            )
                        }
                    }

                resultsDeferred.await()
                    .onSuccess { allResults ->
                        val sortedJornadas = allResults.keys.sorted()
                        _uiState.update {
                            it.copy(
                                allMatchResults = allResults,
                                jornadas = sortedJornadas,
                                selectedJornada = findDefaultJornada(sortedJornadas, allResults),
                                isLoadingResults = false
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingResults = false,
                                resultsError = throwable.message ?: "Error al cargar resultados"
                            )
                        }
                    }
            }
        }
    }

    private fun findDefaultJornada(
        sortedJornadas: List<Int>,
        allResults: Map<Int, List<MatchResult>>
    ): Int? {
        // Pick the latest jornada that has at least one played match.
        val lastWithResults = sortedJornadas.lastOrNull { jornada ->
            allResults[jornada]?.any { it.localScore != "--" && it.visitorScore != "--" } == true
        }

        // If no jornada has results yet, fall back to the first available jornada.
        return lastWithResults ?: sortedJornadas.firstOrNull()
    }

    fun selectJornada(jornada: Int) {
        _uiState.update { it.copy(selectedJornada = jornada) }
    }

    fun toggleFavorite(): Boolean = FavoritesManager.toggleFavorite(groupId)

    fun retryStandings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStandings = true, standingsError = null) }

            runCatching { repository.refreshStandings(groupId) }
                .onSuccess { standings ->
                    _uiState.update {
                        it.copy(
                            standings = standings,
                            isLoadingStandings = false,
                            standingsError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingStandings = false,
                                standingsError = throwable.message
                                    ?: "No se pudo cargar la clasificacion"
                        )
                    }
                }
        }
    }

    fun retryResults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingResults = true, resultsError = null) }
            runCatching { repository.getAllMatchResults(groupId) }
                .onSuccess { allResults ->
                    val sortedJornadas = allResults.keys.sorted()
                    _uiState.update {
                        it.copy(
                            allMatchResults = allResults,
                            jornadas = sortedJornadas,
                            selectedJornada = findDefaultJornada(sortedJornadas, allResults),
                            isLoadingResults = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingResults = false,
                            resultsError = throwable.message ?: "Error al cargar resultados"
                        )
                    }
                }
        }
    }
}

class GroupDetailViewModelFactory(
    private val groupId: Int,
    private val groupName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GroupDetailViewModel(groupId, groupName) as T
    }
}
