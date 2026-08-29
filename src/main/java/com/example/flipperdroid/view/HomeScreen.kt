package com.example.flipperdroid.view

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    nfcViewModel: NfcViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("legal_prefs", Context.MODE_PRIVATE) }
    var legalAccepted by remember { mutableStateOf(prefs.getBoolean("legalAcceptedV2", false)) }

    if (!legalAccepted) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("FlipperDroid V2 — Authorized use") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use FlipperDroid only on devices, tags and networks you own or are explicitly authorized to test.")
                    Text("V2 starts in safe-by-default mode. High-risk experimental modules are not exposed from the main navigation.")
                    OutlinedButton(
                        onClick = { navController.navigate("legal_cgu") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Read Terms of Use") }
                    OutlinedButton(
                        onClick = { navController.navigate("legal_mentions") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Read Legal Notice") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit { putBoolean("legalAcceptedV2", true) }
                    legalAccepted = true
                }) { Text("I understand") }
            }
        )
    }

    val tools = listOf(
        FeatureItem("NFC Reader", "Inspect compatible NFC tags and export scan data.", Icons.Default.Nfc, "nfc"),
        FeatureItem("Network Diagnostics", "Ping, DNS and route checks for authorized networks.", Icons.Default.Router, "network"),
        FeatureItem("Infrared", "Use supported IR hardware with your own devices.", Icons.Default.SettingsRemote, "ir"),
        FeatureItem("Password Generator", "Generate strong random passwords locally.", Icons.Default.Key, "password_generator")
    )

    val appItems = listOf(
        FeatureItem("Device Status", "Check NFC, IR, network and Android compatibility.", Icons.Default.PhoneAndroid, "device_status"),
        FeatureItem("Settings", "Theme and application preferences.", Icons.Default.Settings, "settings"),
        FeatureItem("About V2", "Version, credits and V2 safety model.", Icons.Default.Info, "about")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FlipperDroid V2", fontWeight = FontWeight.Bold)
                        Text("Security toolkit · safe by default", style = MaterialTheme.typography.labelSmall)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(36.dp))
                        Column {
                            Text("V2 baseline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Cleaner navigation, broader Android compatibility and reduced exposure of high-risk modules.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(tools) { feature ->
                FeatureCard(feature) { navController.navigate(feature.route) }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Application", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(appItems) { feature ->
                FeatureCard(feature) { navController.navigate(feature.route) }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(feature.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
