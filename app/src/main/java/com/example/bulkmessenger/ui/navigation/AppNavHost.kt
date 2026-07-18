package com.example.bulkmessenger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bulkmessenger.ui.compose.BroadcastScreen
import com.example.bulkmessenger.ui.compose.PersonalizedRowEditorScreen
import com.example.bulkmessenger.ui.compose.PersonalizedScreen
import com.example.bulkmessenger.ui.drafts.DraftsScreen
import com.example.bulkmessenger.ui.home.HomeScreen
import com.example.bulkmessenger.ui.jobs.JobHistoryScreen
import com.example.bulkmessenger.ui.settings.SettingsScreen
import com.example.bulkmessenger.ui.theme.NavAnimations
import com.example.bulkmessenger.viewmodel.PersonalizedViewModel
import com.example.bulkmessenger.viewmodel.SessionViewModel
import com.example.bulkmessenger.viewmodel.ThemeMode

object Routes {
    const val HOME = "home"
    const val BROADCAST = "broadcast"
    const val PERSONALIZED_GRAPH = "personalized_graph"
    const val PERSONALIZED = "personalized"
    const val PERSONALIZED_ROW_EDITOR = "personalized_row_editor"
    const val DRAFTS = "drafts"
    const val JOBS = "jobs"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel,
    navController: NavHostController = rememberNavController(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = NavAnimations.enter,
        exitTransition = NavAnimations.exit,
        popEnterTransition = NavAnimations.popEnter,
        popExitTransition = NavAnimations.popExit
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewBroadcast = { navController.navigate(Routes.BROADCAST) },
                onNewPersonalized = { navController.navigate(Routes.PERSONALIZED_GRAPH) },
                onDrafts = { navController.navigate(Routes.DRAFTS) },
                onJobHistory = { navController.navigate(Routes.JOBS) },
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
                sessionViewModel = sessionViewModel,
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.BROADCAST) {
            BroadcastScreen(
                sessionViewModel = sessionViewModel,
                onDone = { navController.navigate(Routes.JOBS) { popUpTo(Routes.HOME) } },
                onBack = { navController.popBackStack() }
            )
        }

        // Nested graph so the list page and the add/edit-row page share one PersonalizedViewModel instance.
        navigation(startDestination = Routes.PERSONALIZED, route = Routes.PERSONALIZED_GRAPH) {
            composable(Routes.PERSONALIZED) {
                val parentEntry = navController.getBackStackEntry(Routes.PERSONALIZED_GRAPH)
                val viewModel: PersonalizedViewModel = viewModel(parentEntry)
                PersonalizedScreen(
                    viewModel = viewModel,
                    sessionViewModel = sessionViewModel,
                    onDone = { navController.navigate(Routes.JOBS) { popUpTo(Routes.HOME) } },
                    onBack = { navController.popBackStack() },
                    onAddRow = { navController.navigate(Routes.PERSONALIZED_ROW_EDITOR) },
                    onEditRow = { rowId -> navController.navigate("${Routes.PERSONALIZED_ROW_EDITOR}?rowId=$rowId") }
                )
            }
            composable(
                route = "${Routes.PERSONALIZED_ROW_EDITOR}?rowId={rowId}",
                arguments = listOf(navArgument("rowId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val parentEntry = navController.getBackStackEntry(Routes.PERSONALIZED_GRAPH)
                val viewModel: PersonalizedViewModel = viewModel(parentEntry)
                val rowId = backStackEntry.arguments?.getString("rowId")
                PersonalizedRowEditorScreen(
                    rowId = rowId,
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.DRAFTS) { DraftsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.JOBS) { JobHistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(sessionViewModel = sessionViewModel, onBack = { navController.popBackStack() })
        }
    }
}
