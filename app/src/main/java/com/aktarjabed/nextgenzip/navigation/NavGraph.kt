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
    object Security : Screen("security_dashboard")
    object SecuritySettings : Screen("security_settings")
    object CloudUsage : Screen("cloud_usage")
    object Troubleshooting : Screen("troubleshooting")
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) }
            )
        }
        composable(Screen.Archive.route) {
            ArchiveScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Extract.route) {
            ExtractScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            com.aktarjabed.nextgenzip.ui.screens.AdvancedSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenUsage = { navController.navigate(Screen.CloudUsage.route) },
                onOpenTroubleshoot = { navController.navigate(Screen.Troubleshooting.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen()
        }
        composable(Screen.Security.route) {
            com.aktarjabed.nextgenzip.ui.screens.MalwareDetectionScreen(
                onSettingsClick = { navController.navigate(Screen.SecuritySettings.route) }
            )
        }
        composable(Screen.SecuritySettings.route) {
            com.aktarjabed.nextgenzip.ui.screens.SecuritySettingsScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.CloudUsage.route) {
            com.aktarjabed.nextgenzip.ui.screens.CloudUsageDashboard(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Troubleshooting.route) {
            com.aktarjabed.nextgenzip.ui.screens.TroubleshootingScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
