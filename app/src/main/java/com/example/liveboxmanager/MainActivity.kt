package com.example.liveboxmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.liveboxmanager.ui.screens.*
import com.example.liveboxmanager.ui.theme.LiveboxManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiveboxManagerTheme { App() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Tableau de bord", Icons.Filled.Devices),
    Tab("wifi", "WiFi", Icons.Filled.Wifi),
    Tab("parental", "Parental", Icons.Filled.Shield),
    Tab("firewall", "Pare-feu", Icons.Filled.Security),
)

@Composable
fun App(vm: LiveboxViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (current != null && current != "login") {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { nav.navigate(tab.route) { launchSingleTop = true; restoreState = true } },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (vm.isLoggedIn) "dashboard" else "login",
            modifier = Modifier.padding(padding)
        ) {
            composable("login") { LoginScreen(vm, onLogged = { nav.navigate("dashboard") { popUpTo("login") { inclusive = true } } }) }
            composable("dashboard") { DashboardScreen(vm) }
            composable("wifi") { WifiScreen(vm) }
            composable("parental") { ParentalScreen(vm) }
            composable("firewall") { FirewallScreen(vm) }
        }
    }
}
