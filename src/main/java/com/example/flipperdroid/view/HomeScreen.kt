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
fun HomeScreen(
    navController: NavController,
    nfcViewModel: com.example.flipperdroid.viewmodel.NfcViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("legal_prefs", Context.MODE_PRIVATE) }
    var legalAccepted by remember { mutableStateOf(prefs.getBoolean("legalAcceptedV4", false)) }
    var query by remember { mutableStateOf("") }

    if (!legalAccepted) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("FlipperDroid V4 · Owner Toolkit") },
            text = {
                Text("Use hardware and network tools only on devices, tags and networks you own or are explicitly authorized to test. Hardware-backed labs use dedicated FlipperDroid test protocols and avoid disruptive flooding, credential capture and automatic command injection.")
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit { putBoolean("legalAcceptedV4", true) }
                    legalAccepted = true
                }) { Text("Enter V4") }
            }
        )
    }

    val wireless = listOf(
        FeatureItem("BLE Explorer", "Discover nearby BLE advertisements and inspect GATT on devices you control.", Icons.Default.BluetoothSearching, "bluetooth_scan", "REAL"),
        FeatureItem("BLE Test Beacon", "Broadcast one normal V4 beacon to test a second device.", Icons.Default.BluetoothAudio, "ble_advertiser", "REAL"),
        FeatureItem("Wi‑Fi Analyzer", "Inspect SSID, BSSID, channel, signal and security posture.", Icons.Default.Wifi, "wifi_deauther", "REAL"),
        FeatureItem("LAN Analyzer", "Discover responsive hosts on your connected private network.", Icons.Default.Lan, "lan_analyzer", "REAL"),
        FeatureItem("Network Toolkit", "DNS, ping, routes and targeted connectivity checks.", Icons.Default.Router, "network", "REAL")
    )

    val hardware = listOf(
        FeatureItem("NFC / RFID", "Read NFC metadata, NDEF and supported tag memory.", Icons.Default.Nfc, "nfc", "REAL"),
        FeatureItem("USB Inspector", "Inspect connected USB OTG devices, interfaces and endpoints.", Icons.Default.Usb, "usb_inspector", "REAL"),
        FeatureItem("Infrared Remote", "Transmit IR commands on phones with an IR blaster.", Icons.Default.SettingsRemote, "ir", "DEVICE"),
        FeatureItem("EMV Metadata", "Identify payment application metadata without exposing PAN or Track2.", Icons.Default.CreditCard, "emv_reader", "PRIVATE")
    )

    val labs = listOf(
        FeatureItem("BLE Radio Lab", "Transmit a real FlipperDroid test beacon to a second device; legacy vendor profiles remain preview-only.", Icons.Default.Bluetooth, "ble_payload_lab", "REAL LAB"),
        FeatureItem("USB OTG Lab", "Request real Android USB permission and open/close your connected OTG peripheral.", Icons.Default.Usb, "usb_hid_lab", "REAL LAB"),
        FeatureItem("NFC APDU Lab", "Run real phone-to-phone HCE/ISO-DEP SELECT, PING and ECHO tests.", Icons.Default.Contactless, "emv_emulation", "REAL LAB")
    )

    val utilities = listOf(
        FeatureItem("QR Tools", "Generate and inspect QR payloads locally.", Icons.Default.QrCode, "qr_scanner", "LOCAL"),
        FeatureItem("Password Generator", "Generate strong passwords on-device.", Icons.Default.Key, "password_generator", "LOCAL"),
        FeatureItem("Device Status", "Check NFC, BLE, Wi‑Fi, USB, IR and Android support.", Icons.Default.PhoneAndroid, "device_status", "CHECK"),
        FeatureItem("Settings", "Theme and application preferences.", Icons.Default.Settings, "settings", "APP"),
        FeatureItem("About V4", "Version, modules, limitations and compatibility notes.", Icons.Default.Info, "about", "INFO")
    )

    fun filtered(items: List<FeatureItem>): List<FeatureItem> {
        val q = query.trim()
        if (q.isBlank()) return items
        return items.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.badge.contains(q, ignoreCase = true)
        }
    }

    val visibleCount = (wireless + hardware + labs + utilities).count {
        val q = query.trim()
        q.isBlank() || it.title.contains(q, true) || it.subtitle.contains(q, true) || it.badge.contains(q, true)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(11.dp).size(34.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("FLIPPERDROID", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Text("V4.0 · OWNER TOOLKIT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                            AssistChip(onClick = {}, label = { Text("V4") })
                        }
                        Text(
                            "Hardware diagnostics, wireless inspection and real device-to-device test labs in one Android toolkit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusMini("REAL", Icons.Default.Bolt)
                            StatusMini("LAB", Icons.Default.Science)
                            StatusMini("LOCAL", Icons.Default.PhoneAndroid)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search tools and labs") },
                    supportingText = { Text("$visibleCount module(s) available") }
                )
            }

            val wirelessVisible = filtered(wireless)
            if (wirelessVisible.isNotEmpty()) {
                item { SectionTitle("WIRELESS", Icons.Default.Wifi) }
                items(wirelessVisible, key = { it.route }) { FeatureCard(it) { navController.navigate(it.route) } }
            }

            val hardwareVisible = filtered(hardware)
            if (hardwareVisible.isNotEmpty()) {
                item { SectionTitle("HARDWARE", Icons.Default.Memory) }
                items(hardwareVisible, key = { it.route }) { FeatureCard(it) { navController.navigate(it.route) } }
            }

            val labsVisible = filtered(labs)
            if (labsVisible.isNotEmpty()) {
                item { SectionTitle("LABS", Icons.Default.Science) }
                items(labsVisible, key = { it.route }) { FeatureCard(it) { navController.navigate(it.route) } }
            }

            val utilitiesVisible = filtered(utilities)
            if (utilitiesVisible.isNotEmpty()) {
                item { SectionTitle("UTILITIES", Icons.Default.Build) }
                items(utilitiesVisible, key = { it.route }) { FeatureCard(it) { navController.navigate(it.route) } }
            }

            if (visibleCount == 0) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("No module matches ‘$query’", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { query = "" }) { Text("Clear search") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMini(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(11.dp).size(27.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Text(
                            feature.badge,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(feature.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
