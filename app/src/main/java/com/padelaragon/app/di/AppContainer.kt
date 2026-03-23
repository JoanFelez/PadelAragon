package com.padelaragon.app.di

import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.repository.LeagueRepository
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import com.padelaragon.app.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.data.repository.datasource.TeamDataSource

class AppContainer(database: AppDatabase) {
    private val leagueRepository = LeagueRepository(database)

    val groupDataSource: GroupDataSource = leagueRepository
    val standingsDataSource: StandingsDataSource = leagueRepository
    val matchResultDataSource: MatchResultDataSource = leagueRepository
    val teamDataSource: TeamDataSource = leagueRepository
    val matchDetailDataSource: MatchDetailDataSource = leagueRepository
    val favoritesDataSource: FavoritesDataSource = FavoritesManager
}
