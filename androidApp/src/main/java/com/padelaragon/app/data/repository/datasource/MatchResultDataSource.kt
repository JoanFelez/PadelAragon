package com.padelaragon.app.data.repository.datasource

import com.padelaragon.app.data.model.MatchResult

interface MatchResultDataSource {
    suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult>
    suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>>
    suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>>
    suspend fun getJornadas(groupId: Int): List<Int>
}
