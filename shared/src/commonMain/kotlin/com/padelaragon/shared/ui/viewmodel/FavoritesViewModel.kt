package com.padelaragon.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.data.repository.datasource.FavoritesDataSource
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.domain.usecase.SortGroupsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Favourite groups across every league, in league order. */
class FavoritesViewModel(
    private val sources: List<LeagueSources>,
    private val sortGroupsUseCase: SortGroupsUseCase = SortGroupsUseCase()
) : ViewModel() {

    class LeagueSources(
        val league: League,
        val groups: GroupDataSource,
        val favorites: FavoritesDataSource
    )

    data class Entry(val league: League, val group: LeagueGroup)

    data class UiState(
        val entries: List<Entry> = emptyList(),
        val isLoading: Boolean = true
    )

    val uiState: StateFlow<UiState> = combine(
        sources.map { source ->
            combine(source.favorites.favorites, source.groups.observeGroups()) { favorites, groups ->
                sortGroupsUseCase(groups.filter { it.id in favorites }).map { Entry(source.league, it) }
            }
        }
    ) { perLeague -> UiState(entries = perLeague.flatMap { it }, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        // A favourite can exist before the league's groups were ever cached; make sure they get loaded.
        sources.forEach { source ->
            if (source.favorites.favorites.value.isNotEmpty()) {
                viewModelScope.launch { runCatching { source.groups.getGroups() } }
            }
        }
    }
}

class FavoritesViewModelFactory(
    private val sources: List<FavoritesViewModel.LeagueSources>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
        require(modelClass == FavoritesViewModel::class) { "Unknown ViewModel class: $modelClass" }
        return FavoritesViewModel(sources) as T
    }

    companion object {
        fun from(container: MultiLeagueContainer) = FavoritesViewModelFactory(
            container.leagues.value.map { league ->
                val leagueContainer = container[league]
                FavoritesViewModel.LeagueSources(
                    league, leagueContainer.groupDataSource, leagueContainer.favoritesDataSource
                )
            }
        )
    }
}
