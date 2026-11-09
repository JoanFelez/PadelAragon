package com.padelaragon.shared.data.repository.datasource

import com.padelaragon.shared.data.model.LeagueGroup
import kotlinx.coroutines.flow.Flow

interface GroupDataSource {
    fun observeGroups(): Flow<List<LeagueGroup>>
    suspend fun getGroups(): List<LeagueGroup>
    suspend fun refreshGroups(): List<LeagueGroup>
    suspend fun prefetchAllGroups()
    suspend fun prefetchGroups(groupIds: List<Int>)
}
