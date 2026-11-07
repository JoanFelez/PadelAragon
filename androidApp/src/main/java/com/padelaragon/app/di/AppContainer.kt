package com.padelaragon.app.di

import com.padelaragon.app.data.favorites.FavoritesManager
import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.network.HtmlFetcher
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

class AppContainer(database: AppDatabase, cacheDir: java.io.File? = null) {
    private val scraping = ScrapingService(database, HtmlFetcher(cacheDir))
    private val standingsRepository = StandingsRepository(scraping)
    private val matchResultRepository = MatchResultRepository(scraping)
    private val matchDetailRepository = MatchDetailRepository(scraping)
    private val groupRepository = GroupRepository(scraping, standingsRepo = standingsRepository, matchResultRepo = matchResultRepository)
    private val teamDetailRepository = TeamDetailRepository(
        scraping,
        groupDataSource = groupRepository,
        standingsDataSource = standingsRepository,
        matchResultDataSource = matchResultRepository
    )

    val groupDataSource: GroupDataSource = groupRepository
    val standingsDataSource: StandingsDataSource = standingsRepository
    val matchResultDataSource: MatchResultDataSource = matchResultRepository
    val teamDataSource: TeamDataSource = teamDetailRepository
    val matchDetailDataSource: MatchDetailDataSource = matchDetailRepository
    val favoritesDataSource: FavoritesDataSource = FavoritesManager
    val prefetchGroupsUseCase = PrefetchGroupsUseCase(groupRepository, standingsRepository, matchResultRepository)
}
