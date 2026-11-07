package com.padelaragon.shared.domain.notification

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.sync.DataSourceMode
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

/**
 * One polling pass shared by Android's worker and the desktop loop: refresh data, then turn what
 * changed in favourite groups into [NotificationEvent]s. Never throws except on cancellation.
 */
class ResultNotifier(
    private val container: MultiLeagueContainer,
    private val settings: NotificationSettings,
    private val detector: ChangeDetector,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    suspend fun check(): List<NotificationEvent> {
        if (!settings.enabled) return emptyList()
        val events = mutableListOf<NotificationEvent>()
        try {
            val leagues = container.leagues.value.ifEmpty { League.entries }
            val withFavorites = leagues.filter { container[it].favoritesDataSource.favorites.value.isNotEmpty() }
            val backend = container.syncSettings.let { it.mode != DataSourceMode.DEVICE && it.backendConfig.isComplete }
            for (league in withFavorites) {
                val app = container[league]
                if (backend) {
                    container.syncEngine.sync(league)
                } else if (nowMs() - settings.lastScrapeAtMs >= NotificationSettings.SCRAPE_MIN_INTERVAL_MS) {
                    app.groupDataSource.getGroups()
                    app.favoritesDataSource.favorites.value.forEach { id ->
                        runCatching {
                            app.standingsDataSource.refreshStandings(id)
                            app.matchResultDataSource.refreshMatchResults(id)
                        }.onFailure { if (it is CancellationException) throw it }
                    }
                }
                val groups = app.groupDataSource.getGroups().associateBy { it.id }
                val favorites = app.favoritesDataSource.favorites.value
                val data = favorites.associateWith { id ->
                    ChangeDetector.GroupData(
                        groups[id]?.name ?: "Grupo $id",
                        app.matchResultDataSource.getAllMatchResults(id).values.flatten(),
                        app.standingsDataSource.getStandings(id)
                    )
                }
                events += detector.detectFavorites(league.id, favorites, data)
            }
            if (!backend && withFavorites.isNotEmpty() &&
                nowMs() - settings.lastScrapeAtMs >= NotificationSettings.SCRAPE_MIN_INTERVAL_MS
            ) settings.lastScrapeAtMs = nowMs()
            settings.lastCheckAtMs = nowMs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("ResultNotifier", "Check failed: ${e.message}")
        }
        return events
    }

    /** Runs [check] only when the configured interval elapsed since the last pass. */
    suspend fun checkIfDue(): List<NotificationEvent> {
        val intervalMs = settings.intervalHours * 60 * 60 * 1000L
        return if (nowMs() - settings.lastCheckAtMs >= intervalMs) check() else emptyList()
    }
}
