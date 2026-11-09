package com.padelaragon.shared.data.repository.datasource

import com.padelaragon.shared.data.model.MatchResult
import kotlinx.coroutines.flow.Flow

interface MatchResultDataSource {
    /** Emits every cached result of the group, keyed by jornada, and later local changes. */
    fun observeMatchResults(groupId: Int): Flow<Map<Int, List<MatchResult>>>
    suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult>
    suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>>

    /** Re-fetches every jornada that is not yet finalized. */
    suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>>
    suspend fun getJornadas(groupId: Int): List<Int>
}
