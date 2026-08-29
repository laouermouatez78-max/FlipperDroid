package com.example.flipperdroid.view

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, themeViewModel: ThemeViewModel) {
    val darkMode by themeViewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    fun openSettings(action: String) {
        runCatching { context.startActivity(Intent(action)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("APPEARANCE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Dark cyber theme") },
                        supportingContent = { Text("V4 keeps the Flipper orange/graphite palette instead of Android dynamic colors.") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = { Switch(checked = darkMode, onCheckedChange = themeViewModel::setDarkMode) }
                    )
                }
            }

            item { Text("ANDROID RADIOS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            item { SettingsShortcut("Wi‑Fi settings", "Enable Wi‑Fi/location before scanning", Icons.Default.Wifi) { openSettings(Settings.ACTION_WIFI_SETTINGS) } }
            item { SettingsShortcut("Bluetooth settings", "Enable Bluetooth and manage paired devices", Icons.Default.Bluetooth) { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) } }
            item { SettingsShortcut("NFC settings", "Enable NFC for tag tools", Icons.Default.Nfc) { openSettings(Settings.ACTION_NFC_SETTINGS) } }

            item { Text("APP PERMISSIONS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            item {
                SettingsShortcut("FlipperDroid permissions", "Manage Nearby devices, Location and other permissions", Icons.Default.AdminPanelSettings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
private fun SettingsShortcut(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
        )
    }
}
