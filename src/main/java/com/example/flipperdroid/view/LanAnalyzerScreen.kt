package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.LanAnalyzerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanAnalyzerScreen(navController: NavController, viewModel: LanAnalyzerViewModel) {
    val localIp by viewModel.localIp.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val hosts by viewModel.hosts.collectAsState()
    val scanning by viewModel.isScanning.collectAsState()
    val status by viewModel.status.collectAsState()

    DisposableEffect(Unit) {
        viewModel.refreshLocalIp()
        onDispose { viewModel.stopScan() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAN Analyzer · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshLocalIp, enabled = !scanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh network")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                Icons.Default.Lan,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp).size(28.dp)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Private network", fontWeight = FontWeight.Bold)
                            Text(networkType, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("Local IPv4: ${localIp ?: "not detected"}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                if (scanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = viewModel::stopScan, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop LAN scan")
                        }
                    }
                } else {
                    Button(
                        onClick = viewModel::scanLocalSubnet,
                        enabled = localIp != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan my private /24")
                    }
                }
            }

            if (hosts.isEmpty() && !scanning) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (localIp == null) Icons.Default.WifiOff else Icons.Default.Devices,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (localIp == null) "Connect to private Wi‑Fi/Ethernet" else "No LAN scan results yet",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "V4 scans only the private /24 attached to this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (hosts.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${hosts.size} HOST(S)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }
            }

            items(hosts, key = { it.ip }) { host ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(host.ip, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text("ONLINE") })
                        }
                        host.hostname?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(
                            if (host.openPorts.isEmpty()) "Responsive · no checked common TCP service answered"
                            else "Common services responding: ${host.openPorts.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Use LAN discovery only on a network you own or are explicitly authorized to assess.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
