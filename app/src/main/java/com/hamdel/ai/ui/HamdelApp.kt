package com.hamdel.ai.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hamdel.ai.ui.screens.AnalysisScreen
import com.hamdel.ai.ui.screens.AssistantScreen
import com.hamdel.ai.ui.screens.ConversationsScreen
import com.hamdel.ai.ui.screens.DashboardScreen
import com.hamdel.ai.ui.screens.ProfileScreen
import com.hamdel.ai.ui.screens.SessionsScreen

private data class Destination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

private val destinations = listOf(
    Destination("dashboard", "داشبورد", { Icon(Icons.Outlined.Dashboard, contentDescription = null) }),
    Destination("conversations", "گفتگوها", { Icon(Icons.Outlined.Chat, contentDescription = null) }),
    Destination("sessions", "جلسات", { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) }),
    Destination("analysis", "تحلیل", { Icon(Icons.Outlined.Analytics, contentDescription = null) }),
    Destination("assistant", "دستیار", { Icon(Icons.Outlined.Psychology, contentDescription = null) }),
    Destination("profile", "پروفایل", { Icon(Icons.Outlined.Person, contentDescription = null) })
)

@Composable
fun HamdelApp(viewModel: RelationshipViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = destination.icon,
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier,
            builder = {
                composable("dashboard") { DashboardScreen(viewModel, padding) }
                composable("conversations") { ConversationsScreen(viewModel, padding) }
                composable("sessions") { SessionsScreen(viewModel, padding) }
                composable("analysis") { AnalysisScreen(viewModel, padding) }
                composable("assistant") { AssistantScreen(viewModel, padding) }
                composable("profile") { ProfileScreen(viewModel, padding) }
            }
        )
    }
}
