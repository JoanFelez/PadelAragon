package com.padelaragon.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.padelaragon.app.PadelAragonApp
import com.padelaragon.app.ui.screen.GroupDetailScreen
import com.padelaragon.app.ui.screen.GroupListScreen
import com.padelaragon.app.ui.screen.TeamScreen
import com.padelaragon.app.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.app.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.app.ui.viewmodel.TeamViewModelFactory

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as PadelAragonApp
    val container = app.container

    val navigateToTeam: (Int, String, Int) -> Unit = { teamId, teamName, groupId ->
        navController.navigate("team/$teamId/${Uri.encode(teamName)}/$groupId")
    }

    NavHost(navController = navController, startDestination = "groups") {
        composable("groups") {
            GroupListScreen(
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
