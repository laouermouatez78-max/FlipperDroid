package com.example.flipperdroid.view

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.NfcViewModel

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val labOnly: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    nfcViewModel: NfcViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("legal_prefs", Context.MODE_PRIVATE) }
    var legalAccepted by remember { mutableStateOf(prefs.getBoolean("legalAcceptedV3", false)) }

    if (!legalAccepted) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("FlipperDroid V3 — Authorized Lab") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use FlipperDroid only with hardware, tags, cards and networks you own or are explicitly authorized to test.")
                    Text("V3 restores the complete toolbox. Sensitive modules run in controlled lab/simulation mode to prevent accidental disruption.")
                    OutlinedButton(
                        onClick = { navController.navigate("legal_cgu") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Read Terms of Use") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit { putBoolean("legalAcceptedV3", true) }
                    legalAccepted = true
                }) { Text("I understand") }
            }
        )
    }

    val radioTools = listOf(
        FeatureItem("NFC / RFID", "Read NFC tags, technologies and NDEF metadata.", Icons.Default.Nfc, "nfc"),
        FeatureItem("BLE Lab", "Inspect BLE profiles and simulate controlled advertisement campaigns.", Icons.Default.Bluetooth, "bluetooth", true),
        FeatureItem("Wi‑Fi Audit", "Scan nearby Wi‑Fi, signal, channel and security posture.", Icons.Default.Wifi, "wifi_deauther", true),
        FeatureItem("Infrared", "Transmit IR commands to devices you control.", Icons.Default.SettingsRemote, "ir")
    )

    val securityTools = listOf(
        FeatureItem("Network Toolkit", "DNS, ping, routes and authorized connectivity diagnostics.", Icons.Default.Router, "network"),
        FeatureItem("USB Lab", "Inspect USB HID and validate keyboard scripts without injecting them.", Icons.Default.Usb, "badusb", true),
        FeatureItem("EMV Reader", "Inspect compatible contactless card metadata locally.", Icons.Default.CreditCard, "emv_reader"),
        FeatureItem("EMV Lab", "Simulate EMV APDU flows using test data only.", Icons.Default.Contactless, "emv_emulation", true),
        FeatureItem("Password Generator", "Generate strong random passwords locally.", Icons.Default.Key, "password_generator"),
        FeatureItem("QR Tools", "Inspect and work with QR payloads.", Icons.Default.QrCode, "qr_scanner")
    )

    val appItems = listOf(
        FeatureItem("Device Status", "Check NFC, BLE, Wi‑Fi, IR and Android capabilities.", Icons.Default.PhoneAndroid, "device_status"),
        FeatureItem("Settings", "Theme and application preferences.", Icons.Default.Settings, "settings"),
        FeatureItem("About V3", "Version, modules, credits and lab safety model.", Icons.Default.Info, "about")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FlipperDroid V3", fontWeight = FontWeight.Bold)
                        Text("Cybersecurity multi‑tool · authorized lab", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(40.dp))
                        Column {
                            Text("V3 Full Toolbox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("All major V1/V2 modules are visible again, with cleaner permissions, safer defaults and clearer diagnostics.")
                        }
                    }
                }
            }

            item { SectionTitle("Radio & hardware") }
            items(radioTools) { feature -> FeatureCard(feature) { navController.navigate(feature.route) } }

            item { SectionTitle("Security tools") }
            items(securityTools) { feature -> FeatureCard(feature) { navController.navigate(feature.route) } }

            item { SectionTitle("Application") }
            items(appItems) { feature -> FeatureCard(feature) { navController.navigate(feature.route) } }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (feature.labOnly) {
                        SuggestionChip(onClick = {}, label = { Text("LAB") })
                    }
                }
                Text(feature.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
