package com.padelaragon.app.data.repository.datasource

import com.padelaragon.app.data.model.LeagueGroup

interface GroupDataSource {
    suspend fun getGroups(): List<LeagueGroup>
    suspend fun refreshGroups(): List<LeagueGroup>
    suspend fun prefetchAllGroups()
    suspend fun prefetchGroups(groupIds: List<Int>)
}
