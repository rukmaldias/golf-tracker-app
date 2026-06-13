package com.rapsodo.golf.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.rapsodo.golf.ui.players.PlayerDetailScreen
import com.rapsodo.golf.ui.players.PlayerListScreen

sealed class Screen(val route: String) {
    data object PlayerList : Screen("players")
    data object PlayerDetail : Screen("players/{playerId}") {
        fun createRoute(playerId: String) = "players/$playerId"
    }
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.PlayerList.route
    ) {
        composable(Screen.PlayerList.route) {
            PlayerListScreen(
                onPlayerClick = { playerId ->
                    navController.navigate(Screen.PlayerDetail.createRoute(playerId))
                }
            )
        }

        composable(
            route = Screen.PlayerDetail.route,
            arguments = listOf(navArgument("playerId") { type = NavType.StringType })
        ) {
            PlayerDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}