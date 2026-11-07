package com.padelaragon.shared.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.padelaragon.shared.di.MultiLeagueContainer
import com.padelaragon.shared.ui.chat.ChatScreen
import com.padelaragon.shared.ui.screen.FavoritesScreen
import com.padelaragon.shared.ui.screen.GroupsScreen
import com.padelaragon.shared.ui.screen.LeagueChooserScreen
import com.padelaragon.shared.ui.screen.PlayerDetailScreen
import com.padelaragon.shared.ui.screen.SettingsScreen
import com.padelaragon.shared.ui.screen.TeamScreen
import com.padelaragon.shared.ui.viewmodel.FavoritesViewModelFactory
import com.padelaragon.shared.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.shared.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.shared.ui.viewmodel.PlayerDetailViewModelFactory
import com.padelaragon.shared.ui.viewmodel.TeamViewModelFactory

enum class AppTab(val label: String, val icon: ImageVector) {
    LEAGUES("Ligas", Icons.Filled.EmojiEvents),
    FAVORITES("Favoritos", Icons.Filled.Star),
    CHAT("Chat", Icons.Filled.Chat),
    SETTINGS("Ajustes", Icons.Filled.Settings)
}

private fun AppTab.route(): Any = when (this) {
    AppTab.LEAGUES -> LeaguesRoute
    AppTab.FAVORITES -> FavoritesRoute
    AppTab.CHAT -> ChatRoute
    AppTab.SETTINGS -> SettingsRoute
}

/** Tab a destination belongs to, or null for pushed detail pages (team, player) that inherit the previous tab. */
internal fun NavDestination?.tab(fromFavorites: Boolean = false): AppTab? = when {
    this == null -> null
    hasRoute<LeaguesRoute>() -> AppTab.LEAGUES
    hasRoute<GroupsRoute>() -> if (fromFavorites) AppTab.FAVORITES else AppTab.LEAGUES
    hasRoute<FavoritesRoute>() -> AppTab.FAVORITES
    hasRoute<ChatRoute>() -> AppTab.CHAT
    hasRoute<SettingsRoute>() -> AppTab.SETTINGS
    else -> null
}

@Composable
fun AppNavigation(container: MultiLeagueContainer, screens: AppScreens = AppScreens()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val fromFavorites = backStackEntry?.let { entry ->
        if (entry.destination.hasRoute<GroupsRoute>()) entry.toRoute<GroupsRoute>().fromFavorites else false
    } ?: false
    val playerArgs: PlayerArgsStore = viewModel { PlayerArgsStore() }

    var selectedTab by remember { mutableStateOf(AppTab.LEAGUES) }
    LaunchedEffect(destination, fromFavorites) {
        destination.tab(fromFavorites)?.let { selectedTab = it }
    }

    val suiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    // Pushed pages (team, player) take the full screen on phones; tablets/desktop keep the rail.
    val isPushedPage = destination.tab() == null
    val layoutType =
        if (isPushedPage && suiteType == NavigationSuiteType.NavigationBar) NavigationSuiteType.None else suiteType

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route()) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppTab.entries.forEach { tab ->
                item(
                    selected = tab == selectedTab,
                    onClick = { navigateToTab(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        },
        layoutType = layoutType
    ) {
        NavHost(
            navController = navController,
            startDestination = LeaguesRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<LeaguesRoute> {
                val leagues by container.leagues.collectAsState()
                LeagueChooserScreen(leagues = leagues, onLeagueSelected = { league -> navController.navigate(GroupsRoute(league.id)) })
            }

            composable<FavoritesRoute> {
                FavoritesScreen(
                    onGroupClick = { league, groupId ->
                        navController.navigate(GroupsRoute(league.id, groupId, fromFavorites = true))
                    },
                    viewModelFactory = remember { FavoritesViewModelFactory.from(container) }
                )
            }

            composable<ChatRoute> { Box(Modifier.fillMaxSize()) { screens.chat?.invoke() ?: ChatScreen(container, onOpenSettings = { navigateToTab(AppTab.SETTINGS) }) } }
            composable<SettingsRoute> { Box(Modifier.fillMaxSize()) { screens.settings?.invoke() ?: SettingsScreen(container, screens.settingsExtras) } }

            composable<GroupsRoute> { entry ->
                val route = entry.toRoute<GroupsRoute>()
                val league = container.leagueCatalog.byId(route.leagueId) ?: return@composable
                val leagueContainer = container[league]
                GroupsScreen(
                    title = league.displayName,
                    initialGroupId = route.groupId,
                    onBackToLeagues = { navController.popBackStack() },
                    onTeamClick = { teamId, teamName, groupId ->
                        navController.navigate(TeamRoute(league.id, teamId, teamName, groupId))
                    },
                    viewModelKey = "groups_${league.id}",
                    listViewModelFactory = GroupListViewModelFactory(
                        leagueContainer.groupDataSource,
                        leagueContainer.favoritesDataSource,
                        leagueContainer.prefetchGroupsUseCase
                    ),
                    groupDetailViewModelFactory = { groupId, groupName ->
                        GroupDetailViewModelFactory(
                            groupId, groupName,
                            leagueContainer.standingsDataSource,
                            leagueContainer.matchResultDataSource,
                            leagueContainer.matchDetailDataSource,
                            leagueContainer.favoritesDataSource
                        )
                    }
                )
            }

            composable<TeamRoute> { entry ->
                val route = entry.toRoute<TeamRoute>()
                val league = container.leagueCatalog.byId(route.leagueId) ?: return@composable
                val leagueContainer = container[league]
                TeamScreen(
                    teamId = route.teamId,
                    teamName = route.teamName,
                    groupId = route.groupId,
                    league = league,
                    onBack = { navController.popBackStack() },
                    onTeamClick = { teamId, teamName, groupId ->
                        navController.navigate(TeamRoute(league.id, teamId, teamName, groupId))
                    },
                    onPlayerClick = { playerName, matchDetails, playedMatches ->
                        val argsKey = "${league.id}_${route.teamId}_$playerName"
                        playerArgs.put(argsKey, PlayerArgsStore.Args(matchDetails, playedMatches))
                        navController.navigate(PlayerRoute(league.id, route.teamId, playerName, argsKey))
                    },
                    viewModelKey = "team_${league.id}_${route.teamId}",
                    viewModelFactory = TeamViewModelFactory(
                        route.teamId, route.teamName, route.groupId,
                        leagueContainer.teamDataSource,
                        leagueContainer.standingsDataSource,
                        leagueContainer.matchResultDataSource,
                        leagueContainer.matchDetailDataSource
                    )
                )
            }

            composable<PlayerRoute> { entry ->
                val route = entry.toRoute<PlayerRoute>()
                val args = playerArgs.get(route.argsKey)
                if (args == null) {
                    // Process death dropped the in-memory match data; go back to the team page.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    PlayerDetailScreen(
                        playerName = route.playerName,
                        teamId = route.teamId,
                        onBack = { navController.popBackStack() },
                        viewModelKey = "player_${route.argsKey}",
                        viewModelFactory = PlayerDetailViewModelFactory(
                            route.playerName, route.teamId, args.matchDetails, args.playedMatches
                        )
                    )
                }
            }
        }
    }
}
