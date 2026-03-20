package com.padelaragon.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.data.model.TeamInfo
import com.padelaragon.app.data.repository.LeagueRepository
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
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(teamName = teamName))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

    fun retry() = loadTeamInfo()
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
