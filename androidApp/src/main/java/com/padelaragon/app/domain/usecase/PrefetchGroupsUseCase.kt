package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.TeamInfo
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.data.repository.datasource.TeamDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class PrefetchGroupsUseCase(
    private val groupDataSource: GroupDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource
) {
    suspend fun prefetchAll() {
        groupDataSource.prefetchAllGroups()
    }

    suspend fun prefetchFavorites(groupIds: List<Int>) {
        groupDataSource.prefetchGroups(groupIds)
    }
}
