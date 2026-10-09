package de.meintraining.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.meintraining.app.TrainingApp
import de.meintraining.app.data.Repository
import de.meintraining.app.ui.screens.CatalogScreen
import de.meintraining.app.ui.screens.CustomExerciseScreen
import de.meintraining.app.ui.screens.ExerciseDetailScreen
import de.meintraining.app.ui.screens.ExercisePickerScreen
import de.meintraining.app.ui.screens.MusclesScreen
import de.meintraining.app.ui.screens.PhotoScreen
import de.meintraining.app.ui.screens.PlanEditScreen
import de.meintraining.app.ui.screens.PlansScreen
import de.meintraining.app.ui.screens.SettingsEditScreen
import de.meintraining.app.ui.screens.WorkoutScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TRAINING("training", "Training", Icons.Filled.FitnessCenter),
    EXERCISES("uebungen", "Übungen", Icons.AutoMirrored.Filled.ListAlt),
    MUSCLES("muskeln", "Muskeln", Icons.Filled.AccessibilityNew),
}

@Composable
fun rememberRepository(): Repository = (LocalContext.current.applicationContext as TrainingApp).repository

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = Tab.entries.any { it.route == route }

    Scaffold(
        bottomBar = { if (showTabs) TabBar(nav, route) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.TRAINING.route,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Tab.TRAINING.route) {
                PlansScreen(
                    onStart = { nav.navigate("workout/$it") },
                    onEdit = { nav.navigate("plan/$it") },
                )
            }
            composable(Tab.EXERCISES.route) {
                CatalogScreen(
                    onOpen = { nav.navigate("uebung/$it") },
                    onCreateOwn = { nav.navigate("eigene") },
                )
            }
            composable(Tab.MUSCLES.route) {
                MusclesScreen(onOpenExercise = { nav.navigate("uebung/$it") })
            }
            composable("plan/{planId}", listOf(navArgument("planId") { type = NavType.LongType })) {
                val planId = it.arguments!!.getLong("planId")
                PlanEditScreen(
                    planId = planId,
                    onBack = { nav.popBackStack() },
                    onAddExercises = { nav.navigate("plan/$planId/hinzufuegen") },
                    onOpenExercise = { id -> nav.navigate("uebung/$id") },
                )
            }
            composable("plan/{planId}/hinzufuegen", listOf(navArgument("planId") { type = NavType.LongType })) {
                val planId = it.arguments!!.getLong("planId")
                ExercisePickerScreen(
                    planId = planId,
                    onDone = { nav.popBackStack() },
                    onCreateOwn = { nav.navigate("eigene?planId=$planId") },
                )
            }
            composable(
                "eigene?planId={planId}",
                listOf(navArgument("planId") { type = NavType.LongType; defaultValue = -1L }),
            ) {
                val planId = it.arguments!!.getLong("planId").takeIf { id -> id >= 0 }
                CustomExerciseScreen(
                    planId = planId,
                    onBack = { nav.popBackStack() },
                    onCreated = { exerciseId ->
                        if (planId != null) {
                            // Added to the plan straight away: back to the plan, past the picker.
                            nav.popBackStack("plan/{planId}", inclusive = false)
                        } else {
                            nav.navigate("uebung/$exerciseId") { popUpTo("eigene?planId={planId}") { inclusive = true } }
                        }
                    },
                )
            }
            composable("uebung/{id}") {
                val id = it.arguments!!.getString("id")!!
                ExerciseDetailScreen(
                    exerciseId = id,
                    onBack = { nav.popBackStack() },
                    onEditSettings = { nav.navigate("einstellungen/$id") },
                    onShowPhoto = { nav.navigate("foto/$id") },
                )
            }
            composable("einstellungen/{id}") {
                val id = it.arguments!!.getString("id")!!
                SettingsEditScreen(
                    exerciseId = id,
                    onBack = { nav.popBackStack() },
                    onShowPhoto = { nav.navigate("foto/$id") },
                )
            }
            composable("foto/{id}") {
                PhotoScreen(exerciseId = it.arguments!!.getString("id")!!, onBack = { nav.popBackStack() })
            }
            composable("workout/{planId}", listOf(navArgument("planId") { type = NavType.LongType })) {
                WorkoutScreen(
                    planId = it.arguments!!.getLong("planId"),
                    onExit = { nav.popBackStack() },
                    onEditSettings = { id -> nav.navigate("einstellungen/$id") },
                    onShowPhoto = { id -> nav.navigate("foto/$id") },
                )
            }
        }
    }
}

@Composable
private fun TabBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        for (tab in Tab.entries) {
            NavigationBarItem(
                selected = route == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(30.dp)) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Screens below the tab level draw their own top bar, so the outer Scaffold must not pad again. */
val NoInsets = WindowInsets(0, 0, 0, 0)
