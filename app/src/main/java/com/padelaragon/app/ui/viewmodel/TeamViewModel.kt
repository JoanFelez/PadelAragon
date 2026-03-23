package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PlayerStats
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.data.repository.LeagueRepository
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
    private val groupId: Int
) : ViewModel() {
    private val repository = LeagueRepository

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

            runCatching { repository.getTeamInfoForGroup(teamId, teamName, groupId) }
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
                val standingsRefresh = async { runCatching { repository.refreshStandings(groupId) } }
                val resultsRefresh = async { runCatching { repository.refreshMatchResults(groupId) } }
                standingsRefresh.await()
                resultsRefresh.await()
            }

            runCatching { repository.getTeamInfoForGroup(teamId, teamName, groupId) }
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
            val detail = runCatching { repository.getMatchDetail(detailUrl) }.getOrNull()
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
                            val detail = runCatching { repository.getMatchDetail(url) }.getOrNull()
                            if (detail != null) url to detail else null
                        }
                    }
                jobs.forEach { job ->
                    job.await()?.let { (url, detail) -> currentDetails[url] = detail }
                }
            }

            val stats = computePlayerStats(currentDetails)
            _uiState.update {
                it.copy(
                    matchDetails = currentDetails,
                    playerStats = stats,
                    isLoadingStats = false
                )
            }
        }
    }

    private fun computePlayerStats(allDetails: Map<String, MatchDetail>): List<PlayerStats> {
        val playedMatches = _uiState.value.matches.filter { it.localScore != "--" && it.detailUrl != null }
        return Companion.computePlayerStats(allDetails, playedMatches, teamId)
    }

    internal companion object {
        /**
         * Aggregate per-player win/loss/pair statistics from match details.
         *
         * @param allDetails map of detail URL to parsed match detail
         * @param playedMatches matches that have been played (non-"--" scores) and have a detail URL
         * @param teamId the team whose players we're aggregating for
         */
        fun computePlayerStats(
            allDetails: Map<String, MatchDetail>,
            playedMatches: List<MatchResult>,
            teamId: Int
        ): List<PlayerStats> {
            data class Accumulator(
                var wins: Int = 0,
                var losses: Int = 0,
                val pairCounts: MutableMap<Int, Int> = mutableMapOf(),
                var displayName: String = ""
            )

            val statsMap = mutableMapOf<String, Accumulator>()

            for (match in playedMatches) {
                val detail = allDetails[match.detailUrl] ?: continue
                val isLocal = match.localTeamId == teamId

                for (pair in detail.pairs) {
                    if (pair.sets.isEmpty()) continue

                    val localSetsWon = pair.sets.count { it.localScore > it.visitorScore }
                    val visitorSetsWon = pair.sets.count { it.visitorScore > it.localScore }

                    if (localSetsWon == visitorSetsWon) continue

                    val localWins = localSetsWon > visitorSetsWon
                    val ourSideWon = if (isLocal) localWins else !localWins

                    val player1 = if (isLocal) pair.localPlayer1 else pair.visitorPlayer1
                    val player2 = if (isLocal) pair.localPlayer2 else pair.visitorPlayer2

                    for (playerName in listOf(player1, player2)) {
                        val trimmed = playerName.trim()
                        if (trimmed.isEmpty()) continue
                        val key = trimmed.lowercase()

                        val acc = statsMap.getOrPut(key) { Accumulator(displayName = trimmed) }
                        if (ourSideWon) acc.wins++ else acc.losses++
                        acc.pairCounts[pair.pairNumber] = (acc.pairCounts[pair.pairNumber] ?: 0) + 1
                    }
                }
            }

            return statsMap.values
                .map {
                    PlayerStats(
                        name = it.displayName,
                        wins = it.wins,
                        losses = it.losses,
                        pair1Count = it.pairCounts[1] ?: 0,
                        pair2Count = it.pairCounts[2] ?: 0,
                        pair3Count = it.pairCounts[3] ?: 0
                    )
                }
                .sortedWith(compareByDescending<PlayerStats> { it.wins }.thenBy { it.losses })
        }
    }
}

class TeamViewModelFactory(
    private val teamId: Int,
    private val teamName: String,
    private val groupId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TeamViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return TeamViewModel(teamId, teamName, groupId) as T
    }
}
