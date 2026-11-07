package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.League
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Preloads all leagues' groups (+ standings/results) in parallel at app launch, exposing
 * aggregate progress so a startup loading screen can show a progress bar.
 *
 * [preloadLeague] is injected (rather than depending on [com.padelaragon.shared.di.MultiLeagueContainer]
 * directly) so this class can be unit tested with fakes instead of a real Room database.
 */
class StartupPreloader(
    private val leagues: List<League> = League.entries,
    private val preloadLeague: suspend (League) -> Unit
) {

    data class Progress(
        val completedLeagues: Set<League> = emptySet(),
        val failedLeagues: Set<League> = emptySet(),
        val totalLeagues: Int = League.entries.size
    ) {
        val doneCount: Int get() = completedLeagues.size + failedLeagues.size
        val fraction: Float get() = if (totalLeagues == 0) 1f else doneCount.toFloat() / totalLeagues
        val isDone: Boolean get() = doneCount >= totalLeagues
    }

    private val _progress = MutableStateFlow(Progress(totalLeagues = leagues.size))
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    /** Fetches groups + standings + match results for every league, in parallel. Never throws. */
    suspend fun preloadAll(leagues: List<League> = this.leagues) {
        _progress.value = Progress(totalLeagues = leagues.size)
        coroutineScope {
            leagues.forEach { league ->
                launch {
                    val result = runCatching { preloadLeague(league) }
                    _progress.update { current ->
                        if (result.isSuccess) {
                            current.copy(completedLeagues = current.completedLeagues + league)
                        } else {
                            current.copy(failedLeagues = current.failedLeagues + league)
                        }
                    }
                }
            }
        }
    }
}

