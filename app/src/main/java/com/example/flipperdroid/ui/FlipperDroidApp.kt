package com.example.flipperdroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.screens.*
import com.example.flipperdroid.ui.theme.FlipperTheme

data class MainDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val mainDestinations = listOf(
    MainDestination("home", "Accueil", Icons.Default.Home),
    MainDestination("ble", "BLE", Icons.Default.Bluetooth),
    MainDestination("wifi", "Wi‑Fi", Icons.Default.Wifi),
    MainDestination("nfc", "NFC", Icons.Default.Nfc),
    MainDestination("more", "Plus", Icons.Default.MoreHoriz)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipperDroidApp(appViewModel: AppViewModel = viewModel()) {
    val viewModel = appViewModel
    val settings by viewModel.settings.collectAsState()
    FlipperTheme(themeMode = settings.themeMode, accent = settings.accent) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("FlipperDroid") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(viewModel, navController) }
                    composable("ble") { BleScreen(viewModel) }
                    composable("wifi") { WifiScreen(viewModel) }
                    composable("nfc") { NfcScreen(viewModel) }
                    composable("more") { MoreScreen(navController) }
                    composable("network") { NetworkScreen(viewModel) }
                    composable("usb") { UsbScreen(viewModel) }
                    composable("ir") { IrScreen(viewModel) }
                    composable("subghz") { SubGhzScreen(viewModel) }
                    composable("password") { PasswordScreen() }
                    composable("logs") { LogsScreen(viewModel) }
                    composable("settings") { SettingsScreen(viewModel) }
                    composable("lab") { LabScreen(viewModel) }
                    composable("about") { AboutScreen() }
                }
            }
        }
    }
}
