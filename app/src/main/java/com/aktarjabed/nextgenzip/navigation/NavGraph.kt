package com.aktarjabed.nextgenzip.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.nextgenzip.ui.ArchiveScreen
import com.aktarjabed.nextgenzip.ui.ExtractScreen
import com.aktarjabed.nextgenzip.ui.HomeScreen
import com.aktarjabed.nextgenzip.ui.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Archive : Screen("archive")
    object Extract : Screen("extract")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

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
            ArchiveScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Extract.route) {
            ExtractScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
