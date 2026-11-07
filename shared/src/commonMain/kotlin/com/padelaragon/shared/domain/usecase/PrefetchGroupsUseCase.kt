package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.TeamInfo
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import com.padelaragon.shared.data.repository.datasource.MatchResultDataSource
import com.padelaragon.shared.data.repository.datasource.StandingsDataSource
import com.padelaragon.shared.data.repository.datasource.TeamDataSource
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
