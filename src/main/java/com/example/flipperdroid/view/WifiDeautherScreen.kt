package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDeautherScreen(navController: NavController, viewModel: WifiDeautherViewModel) {
    val context = LocalContext.current
    val networks by viewModel.networks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val status by viewModel.status.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPermissionState()
        if (viewModel.permissionsGranted.value) viewModel.startScan()
    }

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wi‑Fi Analyzer · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }, enabled = permissionsGranted && !isScanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Real Android Wi‑Fi scan", fontWeight = FontWeight.Bold)
                        Text("SSID/BSSID, channel, RSSI, frequency and security posture from your phone's Wi‑Fi scanner.")
                    }
                }
            }

            if (!permissionsGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Permissions required", fontWeight = FontWeight.Bold)
                        Text("For Wi‑Fi scan results Android may require both precise location and nearby Wi‑Fi permission. Location services may also need to be enabled on the phone.")
                        Button(onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant Wi‑Fi scan permissions")
                        }
                    }
                }
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (networks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (isScanning) Icons.Default.Wifi else Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text(if (isScanning) "Scanning…" else "No Wi‑Fi results yet", textAlign = TextAlign.Center)
                        if (permissionsGranted && !isScanning) Button(onClick = { viewModel.startScan() }) { Text("Scan now") }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(networks, key = { it.bssid }) { network -> NetworkCard(network, viewModel) }
                }
            }
        }
    }
}

@Composable
fun NetworkCard(network: WifiNetwork, viewModel: WifiDeautherViewModel) {
    val strength = viewModel.getNetworkStrength(network.rssi)
    val security = viewModel.getSecurityType(network.capabilities)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(network.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(network.bssid, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = strength.name,
                    tint = when (strength) {
                        NetworkStrength.EXCELLENT -> MaterialTheme.colorScheme.primary
                        NetworkStrength.GOOD -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        NetworkStrength.FAIR -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        NetworkStrength.POOR -> MaterialTheme.colorScheme.error
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CH ${network.channel} · ${network.frequency} MHz")
                Text("${network.rssi} dBm")
            }
            val securityLabel = when (security) {
                SecurityType.OPEN -> "OPEN"
                SecurityType.WEP -> "WEP"
                SecurityType.WPA -> "WPA"
                SecurityType.WPA2 -> "WPA2"
                SecurityType.WPA3 -> "WPA3"
            }
            AssistChip(onClick = {}, label = { Text(securityLabel) })
            Text(viewModel.securityFinding(network), style = MaterialTheme.typography.bodySmall)
            Text("Last seen: ${network.lastSeen}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
