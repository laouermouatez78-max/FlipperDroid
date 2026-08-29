package com.example.flipperdroid.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val badge: String = "ACTIVE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, nfcViewModel: com.example.flipperdroid.viewmodel.NfcViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("legal_prefs", Context.MODE_PRIVATE) }
    var legalAccepted by remember { mutableStateOf(prefs.getBoolean("legalAcceptedV4", false)) }

    if (!legalAccepted) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("FlipperDroid V4 · Owner Mode") },
            text = {
                Text("Use active tools only with devices, tags and networks you own or are explicitly authorized to test. V4 replaces several V3 simulations with real Android-supported diagnostics and test functions.")
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit { putBoolean("legalAcceptedV4", true) }
                    legalAccepted = true
                }) { Text("Enable V4") }
            }
        )
    }

    val wireless = listOf(
        FeatureItem("BLE Scanner", "Discover real BLE advertisements around you.", Icons.Default.BluetoothSearching, "bluetooth_scan"),
        FeatureItem("BLE Advertiser", "Broadcast a V4 test beacon to another device you control.", Icons.Default.BluetoothAudio, "ble_advertiser"),
        FeatureItem("Wi‑Fi Audit", "Inspect nearby SSID, BSSID, channel, RSSI and security.", Icons.Default.Wifi, "wifi_deauther"),
        FeatureItem("LAN Analyzer", "Discover active hosts on your connected private /24.", Icons.Default.Lan, "lan_analyzer"),
        FeatureItem("Network Toolkit", "DNS, ping, routes and targeted port checks.", Icons.Default.Router, "network")
    )

    val hardware = listOf(
        FeatureItem("NFC / RFID", "Read metadata/NDEF and authorized tag memory.", Icons.Default.Nfc, "nfc"),
        FeatureItem("USB Inspector", "Inspect real OTG USB devices, interfaces and endpoints.", Icons.Default.Usb, "usb_inspector"),
        FeatureItem("Infrared", "Transmit IR commands with compatible phones.", Icons.Default.SettingsRemote, "ir"),
        FeatureItem("EMV Metadata", "Identify payment application metadata without exposing PAN/Track2.", Icons.Default.CreditCard, "emv_reader", "PRIVATE"),
        FeatureItem("APDU Sandbox", "Synthetic ISO‑DEP/APDU experiments with test data.", Icons.Default.Contactless, "emv_emulation", "SANDBOX")
    )

    val utilities = listOf(
        FeatureItem("QR Tools", "Analyze text and generate QR payloads locally.", Icons.Default.QrCode, "qr_scanner"),
        FeatureItem("Password Generator", "Generate strong passwords on-device.", Icons.Default.Key, "password_generator"),
        FeatureItem("Device Status", "Check NFC, BLE, Wi‑Fi, USB, IR and Android support.", Icons.Default.PhoneAndroid, "device_status"),
        FeatureItem("Settings", "Theme and application preferences.", Icons.Default.Settings, "settings"),
        FeatureItem("About V4", "Version, modules and compatibility notes.", Icons.Default.Info, "about", "INFO")
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                            Text("FLIPPERDROID", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                        Text("V4 · ACTIVE OWNER TOOLKIT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Real BLE, Wi‑Fi/LAN, USB, NFC and hardware diagnostics for devices and networks you control.")
                    }
                }
            }

            item { SectionTitle("WIRELESS") }
            items(wireless) { FeatureCard(it) { navController.navigate(it.route) } }
            item { SectionTitle("HARDWARE") }
            items(hardware) { FeatureCard(it) { navController.navigate(it.route) } }
            item { SectionTitle("UTILITIES") }
            items(utilities) { FeatureCard(it) { navController.navigate(it.route) } }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
}

@Composable
private fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Text(feature.badge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(feature.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
