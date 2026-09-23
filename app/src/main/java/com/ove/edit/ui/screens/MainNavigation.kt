package com.ove.edit.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToModelDownload = {
                    navController.navigate("model_download") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("model_download") {
            ModelDownloadScreen(
                onDownloadComplete = {
                    navController.navigate("home") {
                        popUpTo("model_download") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToNewProject = { navController.navigate("new_project") }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable("new_project") {
            NewProjectScreen(onBack = { navController.popBackStack() })
        }
    }
}
