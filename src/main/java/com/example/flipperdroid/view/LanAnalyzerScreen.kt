package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.LanAnalyzerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanAnalyzerScreen(navController: NavController, viewModel: LanAnalyzerViewModel) {
    val localIp by viewModel.localIp.collectAsState()
    val hosts by viewModel.hosts.collectAsState()
    val scanning by viewModel.isScanning.collectAsState()
    val status by viewModel.status.collectAsState()

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
                    IconButton(onClick = viewModel::refreshLocalIp) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh IP")
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
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Connected network", fontWeight = FontWeight.Bold)
                            Text("Local IPv4: ${localIp ?: "not detected"}", fontFamily = FontFamily.Monospace)
                            Text(status, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::scanLocalSubnet,
                    enabled = !scanning && localIp != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (scanning) "Scanning your /24…" else "Scan my local Wi‑Fi/LAN")
                }
                if (scanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            items(hosts) { host ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(host.ip, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        host.hostname?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(
                            if (host.openPorts.isEmpty()) "Reachable · no common TCP port detected"
                            else "Open common ports: ${host.openPorts.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text(
                    "The V4 LAN analyzer targets only the private /24 network your phone is currently connected to. Use it only on a network you own or are authorized to assess.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
