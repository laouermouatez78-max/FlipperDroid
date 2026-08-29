package com.example.flipperdroid.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.NetworkToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(
    navController: NavController,
    viewModel: NetworkToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    var host by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Diagnostics · V2") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearResults() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear results")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Connectivity checks for networks and hosts you are authorized to test.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text("Hostname or IP address") },
                supportingText = { Text("Example: example.com or 192.168.1.1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DiagnosticCard(
                        title = "Ping",
                        subtitle = "Check whether a host is reachable.",
                        icon = { Icon(Icons.Default.NetworkPing, contentDescription = null) },
                        enabled = host.isNotBlank(),
                        onClick = { viewModel.ping(host) }
                    )
                }

                item {
                    DiagnosticCard(
                        title = "DNS Lookup",
                        subtitle = "Resolve a hostname to its addresses.",
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        enabled = host.isNotBlank(),
                        onClick = { viewModel.dnsLookup(host) }
                    )
                }

                item {
                    DiagnosticCard(
                        title = "Route Check",
                        subtitle = "Run the app's basic reachability/path diagnostic.",
                        icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                        enabled = host.isNotBlank(),
                        onClick = { viewModel.traceroute(host) }
                    )
                }

                if (results.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                items(results) { result ->
                    val container = if (result.isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val content = if (result.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = container)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(result.command, style = MaterialTheme.typography.titleSmall, color = content)
                            Text(result.output, style = MaterialTheme.typography.bodySmall, color = content)
                        }
                    }
                }
            }

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = icon
        )
    }
}
