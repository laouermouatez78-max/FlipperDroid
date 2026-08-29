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

/** Kept under the historical class name so existing navigation stays source-compatible. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About FlipperDroid V3") },
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
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("FlipperDroid V3", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                InfoCard("V3 toolbox") {
                    Text("• NFC/RFID metadata + explicit MIFARE read")
                    Text("• Passive BLE scanner")
                    Text("• BLE payload lab simulator")
                    Text("• Wi‑Fi security posture scanner")
                    Text("• Network diagnostics")
                    Text("• USB HID script simulator")
                    Text("• Privacy-first EMV metadata reader")
                    Text("• Synthetic EMV APDU lab")
                    Text("• Infrared, password and QR tools")
                    Text("• Expanded device diagnostics")
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Column {
                            Text("Authorized testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Use the app only on hardware, tags, cards and networks you own or are explicitly authorized to test. High-impact legacy features are preserved as controlled lab simulations.")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Column {
                            Text("Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Based on the open-source FlipperDroid project by Jeremiznoo. This fork keeps upstream attribution and license terms while evolving the Android implementation.")
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
