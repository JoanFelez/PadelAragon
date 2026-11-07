package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PlayerStats
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.data.repository.datasource.TeamDataSource
import com.padelaragon.app.domain.usecase.ComputePlayerStatsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamViewModel(
    private val teamId: Int,
    private val teamName: String,
    private val groupId: Int,
    private val teamDataSource: TeamDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource,
    private val matchDetailDataSource: MatchDetailDataSource,
    private val computePlayerStatsUseCase: ComputePlayerStatsUseCase = ComputePlayerStatsUseCase()
) : ViewModel() {

    data class UiState(
        val teamName: String = "",
        val groupName: String = "",
        val standing: StandingRow? = null,
        val matches: List<MatchResult> = emptyList(),
        val teamDetail: TeamDetail? = null,
        val matchDetails: Map<String, MatchDetail> = emptyMap(),
        val playerStats: List<PlayerStats> = emptyList(),
        val loadingMatchDetails: Set<String> = emptySet(),
        val isLoadingStats: Boolean = false,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(teamName = teamName))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadTeamInfo()
    }

    private fun loadTeamInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching { teamDataSource.getTeamInfoForGroup(teamId, teamName, groupId) }
                .onSuccess { info ->
                    if (info != null) {
                        _uiState.update {
                            it.copy(
                                teamName = info.teamName,
                                groupName = info.groupName,
                                standing = info.standing,
                                matches = info.matches,
                                teamDetail = info.teamDetail,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No se encontró información del equipo"
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Error al cargar datos del equipo"
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.update { it.copy(playerStats = emptyList()) }

            coroutineScope {
                val standingsRefresh = async { runCatching { standingsDataSource.refreshStandings(groupId) } }
                val resultsRefresh = async { runCatching { matchResultDataSource.refreshMatchResults(groupId) } }
                standingsRefresh.await()
                resultsRefresh.await()
            }

            runCatching { teamDataSource.getTeamInfoForGroup(teamId, teamName, groupId) }
                .onSuccess { info ->
                    if (info != null) {
                        _uiState.update {
                            it.copy(
                                teamName = info.teamName,
                                groupName = info.groupName,
                                standing = info.standing,
                                matches = info.matches,
                                teamDetail = info.teamDetail,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(error = "No se encontró información del equipo")
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Error al refrescar datos del equipo")
                    }
                }

            _isRefreshing.value = false
        }
    }

    fun retry() = loadTeamInfo()

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

    fun loadAllMatchDetails() {
        if (_uiState.value.isLoadingStats || _uiState.value.playerStats.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }

            val playedMatches = _uiState.value.matches.filter { it.localScore != "--" && it.detailUrl != null }
            val currentDetails = _uiState.value.matchDetails.toMutableMap()

            coroutineScope {
                val jobs = playedMatches
                    .filter { it.detailUrl!! !in currentDetails }
                    .map { match ->
                        async {
                            val url = match.detailUrl!!
                            val detail = runCatching { matchDetailDataSource.getMatchDetail(url) }.getOrNull()
                            if (detail != null) url to detail else null
                        }
                    }
                jobs.forEach { job ->
                    job.await()?.let { (url, detail) -> currentDetails[url] = detail }
                }
            }

            val stats = computePlayerStatsUseCase(currentDetails, playedMatches, teamId)
            _uiState.update {
                it.copy(
                    matchDetails = currentDetails,
                    playerStats = stats,
                    isLoadingStats = false
                )
            }
        }
    }

    internal companion object {
        /** Kept for backward compatibility with existing tests. */
        fun computePlayerStats(
            allDetails: Map<String, MatchDetail>,
            playedMatches: List<MatchResult>,
            teamId: Int
        ): List<PlayerStats> = ComputePlayerStatsUseCase()(allDetails, playedMatches, teamId)
    }
}

class TeamViewModelFactory(
    private val teamId: Int,
    private val teamName: String,
    private val groupId: Int,
    private val teamDataSource: TeamDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource,
    private val matchDetailDataSource: MatchDetailDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TeamViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return TeamViewModel(
            teamId, teamName, groupId,
            teamDataSource, standingsDataSource,
            matchResultDataSource, matchDetailDataSource
        ) as T
    }
}
