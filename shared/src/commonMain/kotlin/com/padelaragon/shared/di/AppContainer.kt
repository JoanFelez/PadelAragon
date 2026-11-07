package com.padelaragon.shared.di

import com.padelaragon.shared.data.chat.ChatApi
import com.padelaragon.shared.data.chat.ChatPreferences
import com.padelaragon.shared.data.chat.ChatStore
import com.padelaragon.shared.data.chat.RoomChatStore
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
import com.padelaragon.shared.domain.notification.ChangeDetector
import com.padelaragon.shared.domain.notification.NotificationSettings
import com.padelaragon.shared.domain.notification.ResultNotifier
import com.padelaragon.shared.domain.notification.SettingsNotificationStateStore
import com.padelaragon.shared.domain.usecase.PrefetchGroupsUseCase
import com.padelaragon.shared.data.remote.BackendApi
import com.padelaragon.shared.data.sync.BundleImporter
import com.padelaragon.shared.data.sync.LeagueCatalog
import com.padelaragon.shared.data.sync.SyncEngine
import com.padelaragon.shared.data.sync.SyncSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet

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

/**
 * One [AppContainer] per [League], created on first use and shared by the whole UI. Also owns the
 * sync layer: [syncSettings], [leagueCatalog] (league list) and [syncEngine] (backend or scraping).
 */
class MultiLeagueContainer(
    database: AppDatabase,
    private val settings: Settings,
    private val fetcher: HtmlFetcher = HtmlFetcher(),
    api: BackendApi = BackendApi()
) {
    val syncSettings = SyncSettings(settings)
    val chatApi = ChatApi()
    val chatStore: ChatStore = RoomChatStore(database.chatMessageDao())
    val chatPreferences = ChatPreferences(settings)
    private val scraping = ScrapingService(
        database, fetcher, backgroundScrapingAllowed = { syncEngine.backgroundScrapingAllowed() }
    )

    private val containerMap = MutableStateFlow<Map<Int, AppContainer>>(emptyMap())

    val leagueCatalog = LeagueCatalog(api, syncSettings)
    val syncEngine = SyncEngine(
        api = api,
        importer = BundleImporter(database),
        settings = syncSettings,
        scrapeLeague = { league ->
            val leagueContainer = get(league)
            leagueContainer.groupDataSource.getGroups()
            leagueContainer.prefetchGroupsUseCase.prefetchAll()
        }
    )

    val notificationSettings = NotificationSettings(settings)
    val resultNotifier by lazy {
        ResultNotifier(this, notificationSettings, ChangeDetector(SettingsNotificationStateStore(settings)))
    }

    /** Leagues offered by the app (backend list or the built-in fallback). */
    val leagues: StateFlow<List<League>> get() = leagueCatalog.leagues

    operator fun get(league: League): AppContainer =
        containerMap.value[league.id] ?: containerMap.updateAndGet { current ->
            if (league.id in current) current else current + (league.id to AppContainer(league, scraping, settings))
        }.getValue(league.id)

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
