package com.padelaragon.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.sync.SyncKind
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.domain.usecase.StartupPreloader
import com.padelaragon.shared.ui.navigation.AppNavigation
import com.padelaragon.shared.ui.navigation.AppScreens
import com.padelaragon.shared.ui.screen.StartupLoadingScreen
import com.padelaragon.shared.ui.theme.PadelAragonTheme
import kotlinx.coroutines.flow.first

/**
 * Shared entry point used by the Android activity and the desktop window.
 *
 * Startup never blocks on cached installs: cached data is shown right away while favourites
 * are refreshed first and every league afterwards. Only when [blockingFirstRunSync] is set and
 * the local cache is empty (first desktop run) a progress screen is shown until the sync ends.
 */
@Composable
fun App(
    container: MultiLeagueContainer,
    screens: AppScreens = AppScreens(),
    blockingFirstRunSync: Boolean = false
) {
    val preloader = remember { StartupPreloader(container.leagues.value) { league -> syncLeague(container, league) } }
    val progress by preloader.progress.collectAsState()
    var hasCache by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(container) {
        hasCache = hasCachedData(container)
        container.leagueCatalog.refresh()
        val leagues = container.leagues.value
        syncFavoritesFirst(container, leagues)
        preloader.preloadAll(leagues)
    }

    PadelAragonTheme {
        when {
            hasCache == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            blockingFirstRunSync && hasCache == false && !progress.isDone -> StartupLoadingScreen(progress)
            else -> AppNavigation(container, screens)
        }
    }
}

private suspend fun hasCachedData(container: MultiLeagueContainer): Boolean = runCatching {
    container.leagues.value.any { container[it].groupDataSource.observeGroups().first().isNotEmpty() }
}.getOrDefault(false)

private suspend fun syncFavoritesFirst(container: MultiLeagueContainer, leagues: List<League>) {
    leagues.filter { container[it].favoritesDataSource.favorites.value.isNotEmpty() }
        .forEach { container.syncEngine.sync(it) }
}

/** Failure is reported to the preloader so the first-run progress screen can finish. */
private suspend fun syncLeague(container: MultiLeagueContainer, league: League) {
    val result = container.syncEngine.sync(league)
    if (result.kind == SyncKind.FAILED) error(result.message ?: "Sync failed")
}
