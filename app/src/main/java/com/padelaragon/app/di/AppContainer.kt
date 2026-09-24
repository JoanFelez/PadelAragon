package com.padelaragon.app.di

import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.network.HtmlFetcher
import com.padelaragon.app.data.model.League
import com.padelaragon.app.data.repository.GroupRepository
import com.padelaragon.app.data.repository.MatchDetailRepository
import com.padelaragon.app.data.repository.MatchResultRepository
import com.padelaragon.app.data.repository.ScrapingService
import com.padelaragon.app.data.repository.StandingsRepository
import com.padelaragon.app.data.repository.TeamDetailRepository
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import com.padelaragon.app.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.data.repository.datasource.TeamDataSource
import com.padelaragon.app.domain.usecase.PrefetchGroupsUseCase

class AppContainer(
    private val database: AppDatabase,
    private val cacheDir: java.io.File? = null
) {
    private fun createContainer(league: League): LeagueContainer {
        val scraping = ScrapingService(database, HtmlFetcher(cacheDir), league)
        val standingsRepository = StandingsRepository(scraping)
        val matchResultRepository = MatchResultRepository(scraping)
        val matchDetailRepository = MatchDetailRepository(scraping)
        val groupRepository = GroupRepository(scraping, standingsRepo = standingsRepository, matchResultRepo = matchResultRepository)
        val teamDetailRepository = TeamDetailRepository(
            scraping,
            groupDataSource = groupRepository,
            standingsDataSource = standingsRepository,
            matchResultDataSource = matchResultRepository
        )
        return LeagueContainer(
            groupRepository, standingsRepository, matchResultRepository,
            teamDetailRepository, matchDetailRepository
        )
    }

    private val leagueContainers = League.entries.associateWith(::createContainer)
    fun forLeague(league: League): LeagueContainer = leagueContainers.getValue(league)

    private val default = forLeague(League.ABSOLUTA)
    val groupDataSource: GroupDataSource = default.groupDataSource
    val standingsDataSource: StandingsDataSource = default.standingsDataSource
    val matchResultDataSource: MatchResultDataSource = default.matchResultDataSource
    val teamDataSource: TeamDataSource = default.teamDataSource
    val matchDetailDataSource: MatchDetailDataSource = default.matchDetailDataSource
    val favoritesDataSource: FavoritesDataSource = FavoritesManager
    val prefetchGroupsUseCase = default.prefetchGroupsUseCase
}

class LeagueContainer(
    val groupDataSource: GroupDataSource,
    val standingsDataSource: StandingsDataSource,
    val matchResultDataSource: MatchResultDataSource,
    val teamDataSource: TeamDataSource,
    val matchDetailDataSource: MatchDetailDataSource
) {
    val favoritesDataSource: FavoritesDataSource = FavoritesManager
    val prefetchGroupsUseCase = PrefetchGroupsUseCase(
        groupDataSource as GroupRepository,
        standingsDataSource as StandingsRepository,
        matchResultDataSource as MatchResultRepository
    )
}
