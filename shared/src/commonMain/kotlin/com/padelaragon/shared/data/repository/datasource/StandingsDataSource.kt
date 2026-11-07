package com.padelaragon.shared.data.repository.datasource

import com.padelaragon.shared.data.model.StandingRow
import kotlinx.coroutines.flow.Flow

interface StandingsDataSource {
    /** Emits the cached standings and every later change written to the local database. */
    fun observeStandings(groupId: Int): Flow<List<StandingRow>>

    /** Cached standings (stale-while-revalidate); fetches only when nothing is cached. */
    suspend fun getStandings(groupId: Int): List<StandingRow>

    /** Forces a network refresh; on failure throws and leaves the cache untouched. */
    suspend fun refreshStandings(groupId: Int): List<StandingRow>
}
