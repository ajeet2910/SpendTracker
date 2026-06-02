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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
fun TrackerApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard"
    val currentLabel = routes.firstOrNull { it.route == currentRoute }?.label ?: "Transaction Tracker"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Transaction Tracker", modifier = Modifier.padding(24.dp))
                routes.forEach { item ->
                    NavigationDrawerItem(
                        icon = item.icon,
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                popUpTo("dashboard")
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
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    popUpTo("dashboard") {
                                        saveState = true
                                    }
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
                composable("dashboard") { DashboardScreen() }
                composable("accounts") { AccountsScreen() }
                composable("sync") { SyncSmsScreen() }
                composable("reset_sync") { ResetSyncScreen() }
                composable("filters") { FilterTransactionsScreen() }
                composable("transactions") { TransactionsScreen() }
            }
        }
    }
}
