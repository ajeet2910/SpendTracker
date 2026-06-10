package com.transactiontracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

private data class AppRoute(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

private val routes = listOf(
    AppRoute("dashboard", "Dashboard", { Icon(Icons.Outlined.Dashboard, contentDescription = null) }),
    AppRoute("accounts", "Banks & Cards", { Icon(Icons.Outlined.AccountBalance, contentDescription = null) }),
    AppRoute("sync", "Sync SMS", { Icon(Icons.Outlined.Sync, contentDescription = null) }),
    AppRoute("reset_sync", "Reset Sync", { Icon(Icons.Outlined.RestartAlt, contentDescription = null) }),
    AppRoute("filters", "Txn History", { Icon(Icons.Outlined.FilterList, contentDescription = null) }),
    AppRoute("transactions", "Transactions", { Icon(Icons.Outlined.ReceiptLong, contentDescription = null) })
)

private val bottomRoutes = listOf(
    AppRoute("dashboard", "Home", { Icon(Icons.Outlined.Home, contentDescription = null) }),
    AppRoute("filters", "Txn History", { Icon(Icons.Outlined.FilterList, contentDescription = null) })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine the current label based on the destination's route pattern
    val currentLabel = routes.firstOrNull { 
        currentDestination?.hierarchy?.any { h -> h.route?.substringBefore("?") == it.route } == true 
    }?.label ?: "Transaction Tracker"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Transaction Tracker", modifier = Modifier.padding(24.dp))
                routes.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true
                    NavigationDrawerItem(
                        icon = item.icon,
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (item.route == "dashboard") {
                                dashboardViewModel.resetToCurrentMonth()
                            }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentLabel) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomRoutes.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (item.route == "dashboard") {
                                    dashboardViewModel.resetToCurrentMonth()
                                }
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(padding)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToFilters = { year, month, category, direction ->
                            val route = buildString {
                                append("filters")
                                val params = mutableListOf<String>()
                                if (year != null) params.add("year=$year")
                                if (month != null) params.add("month=$month")
                                if (category != null) params.add("category=$category")
                                if (direction != null) params.add("direction=$direction")
                                if (params.isNotEmpty()) {
                                    append("?")
                                    append(params.joinToString("&"))
                                }
                            }
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("accounts") { AccountsScreen() }
                composable("sync") { SyncSmsScreen() }
                composable("reset_sync") { ResetSyncScreen() }
                composable(
                    route = "filters?year={year}&month={month}&category={category}&direction={direction}",
                    arguments = listOf(
                        navArgument("year") { type = NavType.StringType; nullable = true },
                        navArgument("month") { type = NavType.StringType; nullable = true },
                        navArgument("category") { type = NavType.StringType; nullable = true },
                        navArgument("direction") { type = NavType.StringType; nullable = true }
                    )
                ) { backStackEntry ->
                    val year = backStackEntry.arguments?.getString("year")?.toIntOrNull()
                    val month = backStackEntry.arguments?.getString("month")?.toIntOrNull()
                    val category = backStackEntry.arguments?.getString("category")
                    val direction = backStackEntry.arguments?.getString("direction")
                    
                    FilterTransactionsScreen(
                        initialYear = year,
                        initialMonth = month,
                        initialCategory = category,
                        initialDirection = direction
                    )
                }
                composable("transactions") { TransactionsScreen() }
            }
        }
    }
}
