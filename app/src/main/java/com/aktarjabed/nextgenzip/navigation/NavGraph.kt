package com.aktarjabed.nextgenzip.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aktarjabed.nextgenzip.ui.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Archive : Screen("archive")
    object Extract : Screen("extract")
    object Settings : Screen("settings")
    object History : Screen("history")
    object Favorites : Screen("favorites")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                onNavigateToExtract = { navController.navigate(Screen.Extract.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Extract.route) {
            ExtractScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen()
        }
    }
}
