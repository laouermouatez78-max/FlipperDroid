package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About FlipperDroid V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("FlipperDroid V4", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                    Text("Active Owner Toolkit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            item {
                InfoCard("What is real in V4") {
                    Text("• Real BLE scanner + real BLE test advertiser")
                    Text("• Real Wi‑Fi scanner with corrected Android 13+ permissions")
                    Text("• Private-LAN /24 host discovery and common-port checks")
                    Text("• Real USB Host device/interface/endpoint inspection")
                    Text("• NFC metadata, NDEF reading and NDEF text writing")
                    Text("• Explicit authorized MIFARE memory reading")
                    Text("• Network diagnostics, IR, QR and password tools")
                    Text("• Privacy-first EMV application metadata")
                }
            }

            item {
                InfoCard("Android limitations V4 no longer fakes") {
                    Text("• Stock Android USB Host does not automatically make the phone a USB keyboard.")
                    Text("• Wi‑Fi apps cannot reliably force-enable Wi‑Fi on modern Android.")
                    Text("• Payment-card emulation is not exposed as a real card clone.")
                    Text("• Disruptive Wi‑Fi/BLE flooding is not part of Owner Mode.")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Column {
                            Text("Owner / authorized use", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Use active functions only on hardware, tags and networks you own or are explicitly authorized to assess.")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Column {
                            Text("Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Based on the open-source FlipperDroid project by Jeremiznoo. This fork preserves upstream attribution and MIT licensing.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
