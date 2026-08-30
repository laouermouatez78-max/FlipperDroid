package com.example.flipperdroid.view

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("Settings · V5") },
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
            item { SectionLabel("APPEARANCE") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Dark cyber theme") },
                        supportingContent = { Text("V5 keeps the Flipper orange/graphite visual identity instead of dynamic system colors.") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = { Switch(checked = darkMode, onCheckedChange = themeViewModel::setDarkMode) }
                    )
                }
            }

            item { SectionLabel("PRIVACY & SECURITY") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("V5 privacy defaults", fontWeight = FontWeight.Bold)
                        }
                        Text("• Android app backups are disabled for FlipperDroid data.", style = MaterialTheme.typography.bodySmall)
                        Text("• Cleartext HTTP traffic is blocked by the app manifest.", style = MaterialTheme.typography.bodySmall)
                        Text("• File Intelligence analyzes selected files locally; it does not upload them.", style = MaterialTheme.typography.bodySmall)
                        Text("• Network, BLE, IR and USB actions require an explicit user action.", style = MaterialTheme.typography.bodySmall)
                        Text("• OSINT findings and identity links are observations/candidates, not proof of identity.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { SectionLabel("ANDROID RADIOS") }
            item { SettingsShortcut("Wi‑Fi settings", "Enable Wi‑Fi and Location before scanning", Icons.Default.Wifi) { openSettings(Settings.ACTION_WIFI_SETTINGS) } }
            item { SettingsShortcut("Location settings", "Required by Android for Wi‑Fi scan results on supported versions", Icons.Default.LocationOn) { openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
            item { SettingsShortcut("Bluetooth settings", "Enable Bluetooth and manage paired devices", Icons.Default.Bluetooth) { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) } }
            item { SettingsShortcut("NFC settings", "Enable NFC for tag and HCE lab tools", Icons.Default.Nfc) { openSettings(Settings.ACTION_NFC_SETTINGS) } }

            item { SectionLabel("APP PERMISSIONS") }
            item {
                SettingsShortcut("FlipperDroid permissions", "Manage Nearby devices, Location and other Android permissions", Icons.Default.AdminPanelSettings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
            item {
                SettingsShortcut("Device Status", "Review V5 hardware support and action-required states", Icons.Default.FactCheck) {
                    navController.navigate("device_status")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsShortcut(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
        )
    }
}
