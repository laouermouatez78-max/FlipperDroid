package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V4AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About FlipperDroid V5.1") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Icon(Icons.Default.Android, contentDescription = "FlipperDroid V5.1", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)) }
            item { Text("FlipperDroid V5.1", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
            item { Text("Version 5.1.0 · code 6", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                Text(
                    "Android security, hardware diagnostics and public-information analysis toolkit for authorized testing, local analysis and bounded device labs.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            item {
                AboutCard("V5.1 reliability & privacy") {
                    AboutRow(Icons.Default.Security, "App hardening", "Android backups disabled and app cleartext HTTP blocked.")
                    AboutRow(Icons.Default.Key, "Secure secrets", "Password generator uses SecureRandom, sensitive clipboard marking and opt-in password QR display.")
                    AboutRow(Icons.Default.FactCheck, "Readiness diagnostics", "Device Status distinguishes READY, ACTION REQUIRED and UNAVAILABLE states.")
                    AboutRow(Icons.Default.Route, "IPv4 / IPv6", "Network Toolkit accepts hostnames, URLs, IPv4 and IPv6 and reports address scope.")
                }
            }

            item {
                AboutCard("Wireless & network") {
                    AboutRow(Icons.Default.BluetoothSearching, "BLE Explorer", "Passive advertisement decoding plus explicit GATT metadata discovery with a 12-second connection timeout.")
                    AboutRow(Icons.Default.BluetoothAudio, "BLE Test Beacon", "One identifiable V5 beacon with automatic 60-second stop.")
                    AboutRow(Icons.Default.Wifi, "Wi‑Fi Analyzer", "2.4/5/6 GHz bands plus Open, OWE, WEP, WPA, WPA2, WPA2/WPA3 transition and WPA3 classification.")
                    AboutRow(Icons.Default.Lan, "LAN Analyzer", "Live progress/results for the private IPv4 /24 attached to the phone, with a small fixed TCP service set.")
                    AboutRow(Icons.Default.Router, "Network Toolkit", "DNS, reachability, Android route state and six targeted TCP checks on the explicit target.")
                }
            }

            item {
                AboutCard("Hardware") {
                    AboutRow(Icons.Default.Nfc, "NFC / RFID", "Metadata first; explicit NDEF writes and bounded default-key MIFARE reads run off the UI thread.")
                    AboutRow(Icons.Default.Usb, "USB Inspector", "Descriptors, human-readable endpoint types, masked serial numbers and a non-destructive open/close test.")
                    AboutRow(Icons.Default.SettingsRemote, "Infrared", "Strict timing parser and carrier-frequency validation against phone-reported IR ranges.")
                    AboutRow(Icons.Default.CreditCard, "EMV metadata", "PPSE/AID application identification only; PAN, expiry, cardholder name and Track 2 are not requested.")
                }
            }

            item {
                AboutCard("OSINT") {
                    AboutRow(Icons.Default.PersonSearch, "Identity / pseudonym", "Public candidate profiles and search variants without asserting account ownership.")
                    AboutRow(Icons.Default.Dns, "Domain / DNS / RDAP", "Public registry, DNS, TLS and web-surface metadata with bounded explicit requests.")
                    AboutRow(Icons.Default.Shield, "Domain & URL Defense", "Local URL heuristics and public mail-domain posture checks with values redacted where appropriate.")
                    AboutRow(Icons.Default.FilePresent, "File Intelligence", "Local hashes, signatures, entropy, EXIF presence, PDF and Office/package metadata; files are not uploaded.")
                }
            }

            item {
                AboutCard("Bounded labs") {
                    AboutRow(Icons.Default.Radio, "BLE Radio Lab", "Real RF path uses only identifiable FD5LAB data, auto-stop and cooldown; vendor/crash catalogue stays local preview-only.")
                    AboutRow(Icons.Default.Usb, "USB HID Lab", "Real USB permission/open-close checks plus a capped on-screen script preview; no keystroke injection.")
                    AboutRow(Icons.Default.Contactless, "NFC APDU Lab", "Phone-to-phone HCE/ISO-DEP testing for authorized lab use.")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    ListItem(
                        headlineContent = { Text("Authorized / Public Mode") },
                        supportingContent = {
                            Text(
                                "Use active diagnostics only on systems you own or are explicitly authorized to assess. Identity links are candidates, not proof. Heuristic scores are indicators, not verdicts. Stock Android Wi‑Fi APIs do not transmit raw 802.11 deauthentication frames."
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Security, null) }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Build channel") },
                    supportingContent = { Text("FlipperDroid V5.1 · 5.1.0 · debug/review branch") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}

@Composable
private fun AboutCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) }
    )
}
