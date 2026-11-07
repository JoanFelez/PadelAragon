package com.padelaragon.app.data.repository.datasource

import com.padelaragon.app.data.model.StandingRow

interface StandingsDataSource {
    suspend fun getStandings(groupId: Int): List<StandingRow>
    suspend fun refreshStandings(groupId: Int): List<StandingRow>
}
