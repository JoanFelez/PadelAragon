package com.padelaragon.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.padelaragon.app.PadelAragonApp
import com.padelaragon.app.data.model.League
import com.padelaragon.app.ui.screen.GroupDetailScreen
import com.padelaragon.app.ui.screen.LeagueChooserScreen
import com.padelaragon.app.ui.screen.PlayerDetailScreen
import com.padelaragon.app.ui.screen.PossiblePairsInput
import com.padelaragon.app.ui.screen.PossiblePairsScreen
import com.padelaragon.app.ui.screen.GroupListScreen
import com.padelaragon.app.ui.screen.TeamScreen
import com.padelaragon.app.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.app.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.app.ui.viewmodel.TeamViewModelFactory

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as PadelAragonApp
    val preferences = LocalContext.current.getSharedPreferences("league-selection", 0)
    var selectedLeagueId by rememberSaveable {
        mutableStateOf(preferences.getInt("league-id", -1).takeIf { League.fromId(it) != null })
    }
    val selectedLeague = League.fromId(selectedLeagueId)
    var possiblePairsInput by remember { mutableStateOf<PossiblePairsInput?>(null) }

    if (selectedLeague == null) {
        LeagueChooserScreen { league ->
            selectedLeagueId = league.id
            preferences.edit().putInt("league-id", league.id).apply()
        }
        return
    }
    val container = app.container.forLeague(selectedLeague)

    val navigateToTeam: (Int, String, Int) -> Unit = { teamId, teamName, groupId ->
        navController.navigate("team/$teamId/${Uri.encode(teamName)}/$groupId")
    }

    NavHost(navController = navController, startDestination = "groups") {
        composable("groups") {
            GroupListScreen(
                league = selectedLeague,
                onLeagueClick = { selectedLeagueId = null },
                onGroupClick = { groupId, groupName ->
                    navController.navigate("group/$groupId/${Uri.encode(groupName)}")
                },
                viewModelFactory = GroupListViewModelFactory(
                    container.groupDataSource,
                    container.favoritesDataSource,
                    container.prefetchGroupsUseCase
                )
            )
        }

        composable(
            route = "group/{groupId}/{groupName}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.IntType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: return@composable
            val groupName = Uri.decode(backStackEntry.arguments?.getString("groupName") ?: "")

            GroupDetailScreen(
                groupId = groupId,
                groupName = groupName,
                onBack = { navController.popBackStack() },
                onTeamClick = navigateToTeam,
                viewModelFactory = GroupDetailViewModelFactory(
                    groupId, groupName,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource,
                    container.favoritesDataSource
                )
            )
        }

        composable(
            route = "team/{teamId}/{teamName}/{groupId}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("teamName") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getInt("teamId") ?: return@composable
            val teamName = Uri.decode(backStackEntry.arguments?.getString("teamName") ?: "")
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: return@composable

            TeamScreen(
                teamId = teamId,
                teamName = teamName,
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onTeamClick = navigateToTeam,
                onPlayerClick = { playerName ->
                    navController.navigate(
                        "player/$teamId/${Uri.encode(teamName)}/$groupId/${Uri.encode(playerName)}"
                    )
                },
                showPossiblePairsAction = selectedLeague == League.VETERANOS,
                onPossiblePairsClick = { teamDetail, groupName ->
                    possiblePairsInput = PossiblePairsInput(teamDetail, groupName)
                    navController.navigate("possible-pairs")
                },
                viewModelFactory = TeamViewModelFactory(
                    teamId, teamName, groupId,
                    container.teamDataSource,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource
                )
            )
        }

        composable("possible-pairs") {
            PossiblePairsScreen(
                input = possiblePairsInput,
                onBack = { navController.popBackStack() },
                onRetry = { navController.popBackStack() }
            )
        }

        composable(
            route = "player/{teamId}/{teamName}/{groupId}/{playerName}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("teamName") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.IntType },
                navArgument("playerName") { type = NavType.StringType }
            )
        ) { entry ->
            val teamId = entry.arguments?.getInt("teamId") ?: return@composable
            val teamName = Uri.decode(entry.arguments?.getString("teamName") ?: "")
            val groupId = entry.arguments?.getInt("groupId") ?: return@composable
            val playerName = Uri.decode(entry.arguments?.getString("playerName") ?: "")
            PlayerDetailScreen(
                playerName = playerName,
                onBack = { navController.popBackStack() },
                viewModelFactory = TeamViewModelFactory(
                    teamId, teamName, groupId,
                    container.teamDataSource,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource
                )
            )
        }
    }
}
