package com.example.flipperdroid.view

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDeautherScreen(navController: NavController, viewModel: WifiDeautherViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val networks by viewModel.networks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val locationEnabled by viewModel.locationEnabled.collectAsState()
    val wifiReady by viewModel.wifiReady.collectAsState()
    val status by viewModel.status.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPrerequisiteState()
        if (viewModel.permissionsGranted.value && viewModel.locationEnabled.value && viewModel.wifiReady.value) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPrerequisiteState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    IconButton(
                        onClick = { viewModel.startScan() },
                        enabled = permissionsGranted && locationEnabled && wifiReady && !isScanning
                    ) {
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
                        Text("SSID/BSSID, channel, RSSI, frequency and security posture from the phone's Wi‑Fi scanner.")
                    }
                }
            }

            if (!permissionsGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Precise location permission required", fontWeight = FontWeight.Bold)
                        Text("Android requires precise location permission to call Wi‑Fi startScan() and read scan results.")
                        Button(
                            onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Wi‑Fi scan permission")
                        }
                    }
                }
            }

            if (permissionsGranted && !locationEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOff, contentDescription = null)
                            Text("Android Location is off", fontWeight = FontWeight.Bold)
                        }
                        Text("Wi‑Fi scan results are blocked while Location services are disabled.")
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Location settings")
                        }
                    }
                }
            }

            if (permissionsGranted && locationEnabled && !wifiReady) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiOff, contentDescription = null)
                            Text("Wi‑Fi scanning unavailable", fontWeight = FontWeight.Bold)
                        }
                        Text("Turn Wi‑Fi on, then return here and start the scan again.")
                        OutlinedButton(
                            onClick = {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    Intent(Settings.Panel.ACTION_WIFI)
                                } else {
                                    Intent(Settings.ACTION_WIFI_SETTINGS)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Wi‑Fi settings")
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
                        if (permissionsGranted && locationEnabled && wifiReady && !isScanning) {
                            Button(onClick = { viewModel.startScan() }) { Text("Scan now") }
                        }
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
