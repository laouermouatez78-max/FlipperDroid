package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.NetworkToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(navController: NavController, viewModel: NetworkToolsViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var host by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Toolkit · V5") },
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.padding(10.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Rootless IPv4 / IPv6 diagnostics", fontWeight = FontWeight.Bold)
                        Text(
                            "Standard Android/Java networking only: DNS, reachability, route state and six targeted TCP service checks. No root and no broad port scan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = host,
                onValueChange = { host = it.take(500) },
                label = { Text("Hostname, URL, IPv4 or IPv6") },
                supportingText = { Text("Examples: router.local · 192.168.1.1 · 2001:db8::1 · use only authorized targets") },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                trailingIcon = {
                    if (host.isNotBlank()) IconButton(onClick = { host = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear target") }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isScanning
            )

            if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    DiagnosticCard("Reachability", "Resolve the target and run short Java/ping/TCP fallback checks.", Icons.Default.NetworkPing, host.isNotBlank() && !isScanning) { viewModel.ping(host) }
                }
                item {
                    DiagnosticCard("DNS Lookup", "Resolve all current IPv4/IPv6 addresses and classify their scope.", Icons.Default.Language, host.isNotBlank() && !isScanning) { viewModel.dnsLookup(host) }
                }
                item {
                    DiagnosticCard("Android Route Check", "Show transport, interface, gateway, DNS, validation and metered state.", Icons.Default.Route, host.isNotBlank() && !isScanning) { viewModel.traceroute(host) }
                }
                item {
                    DiagnosticCard("Common Services", "Check only SSH, DNS, HTTP, HTTPS, SMB and HTTP-alt on the explicit target.", Icons.Default.Lan, host.isNotBlank() && !isScanning) { viewModel.checkCommonServices(host) }
                }

                if (results.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                    }
                }

                items(results) { result ->
                    val container = if (result.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    val content = if (result.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = container)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (result.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, contentDescription = null, tint = if (result.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Text(result.command, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content, modifier = Modifier.weight(1f))
                                IconButton(onClick = { clipboard.setText(AnnotatedString(result.output)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy result", tint = content)
                                }
                            }
                            SelectionContainer {
                                Text(result.output, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = content)
                            }
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
