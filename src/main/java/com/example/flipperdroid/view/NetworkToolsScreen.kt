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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Toolkit · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearResults, enabled = results.isNotEmpty()) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Rootless Android diagnostics", fontWeight = FontWeight.Bold)
                        Text(
                            "V4 uses standard Android networking APIs. No root, no su, and no dependency on the old bundled Nmap path.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = host,
                onValueChange = { host = it.take(253) },
                label = { Text("Hostname or IP address") },
                supportingText = { Text("Example: router.local or 192.168.1.1 · use only authorized targets") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                trailingIcon = {
                    if (host.isNotBlank()) {
                        IconButton(onClick = { host = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear target")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isScanning
            )

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    DiagnosticCard(
                        title = "Reachability",
                        subtitle = "Resolve the target and run a short reachability check.",
                        icon = Icons.Default.NetworkPing,
                        enabled = host.isNotBlank() && !isScanning,
                        onClick = { viewModel.ping(host) }
                    )
                }
                item {
                    DiagnosticCard(
                        title = "DNS Lookup",
                        subtitle = "Resolve the selected hostname to its current IP addresses.",
                        icon = Icons.Default.Language,
                        enabled = host.isNotBlank() && !isScanning,
                        onClick = { viewModel.dnsLookup(host) }
                    )
                }
                item {
                    DiagnosticCard(
                        title = "Android Route Check",
                        subtitle = "Show interface, gateway, DNS and validation state for the active route.",
                        icon = Icons.Default.Route,
                        enabled = host.isNotBlank() && !isScanning,
                        onClick = { viewModel.traceroute(host) }
                    )
                }
                item {
                    DiagnosticCard(
                        title = "Common Services",
                        subtitle = "Check only six common TCP services on the explicit target.",
                        icon = Icons.Default.Lan,
                        enabled = host.isNotBlank() && !isScanning,
                        onClick = { viewModel.checkCommonServices(host) }
                    )
                }

                if (results.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (result.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (result.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(result.command, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
                            }
                            Text(
                                result.output,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = content
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
        )
    }
}
