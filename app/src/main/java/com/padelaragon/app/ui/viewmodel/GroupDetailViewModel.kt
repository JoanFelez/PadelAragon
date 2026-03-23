package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import com.padelaragon.app.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.domain.usecase.FindDefaultJornadaUseCase
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
    private val groupName: String,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource,
    private val matchDetailDataSource: MatchDetailDataSource,
    private val favoritesDataSource: FavoritesDataSource,
    private val findDefaultJornadaUseCase: FindDefaultJornadaUseCase = FindDefaultJornadaUseCase()
) : ViewModel() {

    data class UiState(
        val groupName: String = "",
        val standings: List<StandingRow> = emptyList(),
        val allMatchResults: Map<Int, List<MatchResult>> = emptyMap(),
        val jornadas: List<Int> = emptyList(),
        val selectedJornada: Int? = null,
        val isLoadingStandings: Boolean = true,
        val isLoadingResults: Boolean = true,
        val standingsError: String? = null,
        val resultsError: String? = null,
        val matchDetails: Map<String, MatchDetail> = emptyMap(),
        val loadingMatchDetails: Set<String> = emptySet()
    )

    private val _uiState = MutableStateFlow(UiState(groupName = groupName))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val currentResults: StateFlow<List<MatchResult>> = uiState
        .map { state -> state.selectedJornada?.let { state.allMatchResults[it] } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val isFavorite: StateFlow<Boolean> = favoritesDataSource.favorites
        .map { favorites -> groupId in favorites }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            favoritesDataSource.isFavorite(groupId)
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
                val standingsDeferred = async { runCatching { standingsDataSource.getStandings(groupId) } }
                val resultsDeferred = async { runCatching { matchResultDataSource.getAllMatchResults(groupId) } }

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
                                selectedJornada = findDefaultJornadaUseCase(sortedJornadas, allResults),
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

    fun selectJornada(jornada: Int) {
        _uiState.update { it.copy(selectedJornada = jornada) }
    }

    fun loadMatchDetail(detailUrl: String) {
        if (detailUrl in _uiState.value.matchDetails || detailUrl in _uiState.value.loadingMatchDetails) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMatchDetails = it.loadingMatchDetails + detailUrl) }
            val detail = runCatching { matchDetailDataSource.getMatchDetail(detailUrl) }.getOrNull()
            _uiState.update { state ->
                state.copy(
                    matchDetails = if (detail != null) state.matchDetails + (detailUrl to detail) else state.matchDetails,
                    loadingMatchDetails = state.loadingMatchDetails - detailUrl
                )
            }
        }
    }

    fun toggleFavorite(): Boolean = favoritesDataSource.toggleFavorite(groupId)

    fun retryStandings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStandings = true, standingsError = null) }

            runCatching { standingsDataSource.refreshStandings(groupId) }
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
            runCatching { matchResultDataSource.getAllMatchResults(groupId) }
                .onSuccess { allResults ->
                    val sortedJornadas = allResults.keys.sorted()
                    _uiState.update {
                        it.copy(
                            allMatchResults = allResults,
                            jornadas = sortedJornadas,
                            selectedJornada = findDefaultJornadaUseCase(sortedJornadas, allResults),
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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            coroutineScope {
                val standingsDeferred = async { runCatching { standingsDataSource.refreshStandings(groupId) } }
                val resultsDeferred = async { runCatching { matchResultDataSource.refreshMatchResults(groupId) } }

                standingsDeferred.await()
                    .onSuccess { standings ->
                        _uiState.update {
                            it.copy(
                                standings = standings,
                                standingsError = null
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                standingsError = throwable.message ?: "Error al refrescar clasificacion"
                            )
                        }
                    }

                resultsDeferred.await()
                    .onSuccess { allResults ->
                        val sortedJornadas = allResults.keys.sorted()
                        _uiState.update { state ->
                            val selected = state.selectedJornada
                                ?.takeIf { it in sortedJornadas }
                                ?: findDefaultJornadaUseCase(sortedJornadas, allResults)

                            state.copy(
                                allMatchResults = allResults,
                                jornadas = sortedJornadas,
                                selectedJornada = selected,
                                resultsError = null
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                resultsError = throwable.message ?: "Error al refrescar resultados"
                            )
                        }
                    }
            }

            _isRefreshing.value = false
        }
    }

    internal companion object {
        /** Kept for backward compatibility with existing tests. */
        fun findDefaultJornada(
            sortedJornadas: List<Int>,
            allResults: Map<Int, List<MatchResult>>
        ): Int? = FindDefaultJornadaUseCase()(sortedJornadas, allResults)
    }
}

class GroupDetailViewModelFactory(
    private val groupId: Int,
    private val groupName: String,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource,
    private val matchDetailDataSource: MatchDetailDataSource,
    private val favoritesDataSource: FavoritesDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GroupDetailViewModel(
            groupId, groupName,
            standingsDataSource, matchResultDataSource,
            matchDetailDataSource, favoritesDataSource
        ) as T
    }
}
