package com.padelaragon.shared.di

import com.padelaragon.shared.data.favorites.createFavoritesSettings
import com.padelaragon.shared.data.local.AppDatabase
import com.padelaragon.shared.data.local.PlatformContext
import com.padelaragon.shared.data.local.appDatabaseBuilder
import com.padelaragon.shared.data.local.buildAppDatabase
import com.padelaragon.shared.data.favorites.FavoritesStore
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.network.HtmlFetcher
import com.padelaragon.shared.data.repository.GroupRepository
import com.padelaragon.shared.data.repository.MatchDetailRepository
import com.padelaragon.shared.data.repository.MatchResultRepository
import com.padelaragon.shared.data.repository.ScrapingService
import com.padelaragon.shared.data.repository.StandingsRepository
import com.padelaragon.shared.data.repository.TeamDetailRepository
import com.padelaragon.shared.data.repository.datasource.FavoritesDataSource
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import com.padelaragon.shared.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.shared.data.repository.datasource.MatchResultDataSource
import com.padelaragon.shared.data.repository.datasource.StandingsDataSource
import com.padelaragon.shared.data.repository.datasource.TeamDataSource
import com.padelaragon.shared.domain.usecase.PrefetchGroupsUseCase
import com.russhwolf.settings.Settings

/**
 * Per-league dependency container. One instance is built per [League], sharing the database,
 * the [ScrapingService] (and thus the fetcher's concurrency limit and in-flight de-duplication)
 * while scoping repositories and Room queries to that league's id.
 */
class AppContainer(val league: League, scraping: ScrapingService, settings: Settings) {
    private val standingsRepository = StandingsRepository(league, scraping)
    private val matchResultRepository = MatchResultRepository(league, scraping)
    private val matchDetailRepository = MatchDetailRepository(scraping)
    private val groupRepository = GroupRepository(
        league, scraping, standingsRepo = standingsRepository, matchResultRepo = matchResultRepository
    )
    private val teamDetailRepository = TeamDetailRepository(
        league,
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
    val favoritesDataSource: FavoritesDataSource = FavoritesStore(league, settings)
    val prefetchGroupsUseCase = PrefetchGroupsUseCase(groupRepository, standingsRepository, matchResultRepository)
}

/** One [AppContainer] per [League], built once at startup and shared by the whole UI. */
class MultiLeagueContainer(
    database: AppDatabase,
    settings: Settings,
    private val fetcher: HtmlFetcher = HtmlFetcher()
) {
    private val scraping = ScrapingService(database, fetcher)

    val containers: Map<League, AppContainer> = League.entries.associateWith { league ->
        AppContainer(league, scraping, settings)
    }

    operator fun get(league: League): AppContainer = containers.getValue(league)

    /** Opens the TLS connection early so the first real request is faster. Never throws. */
    suspend fun prewarmConnection() = fetcher.prewarm("${ScrapingService.BASE_URL}Ligas_Calendario.asp")

    companion object {
        /** Production wiring: platform database file, platform favourites storage, default HTTP client. */
        fun create(context: PlatformContext): MultiLeagueContainer =
            MultiLeagueContainer(
                database = buildAppDatabase(appDatabaseBuilder(context)),
                settings = createFavoritesSettings(context)
            )
    }
}
