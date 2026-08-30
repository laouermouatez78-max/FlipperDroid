package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.OsintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsintScreen(navController: NavController, viewModel: OsintViewModel) {
    val results by viewModel.results.collectAsState()
    val uriHandler = LocalUriHandler.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var publicEmail by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var liveTarget by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OSINT Hub · V5") },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Public-information analysis", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Analyze public names, pseudonyms, domains, DNS, TLS certificates, HTTP security headers, IP addresses and URLs. Active checks are explicit, low-volume and limited to the target you enter.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider()
                        Button(
                            onClick = { navController.navigate("osint_advanced") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ManageSearch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Advanced OSINT")
                        }
                        OutlinedButton(
                            onClick = { navController.navigate("osint_file") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FilePresent, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("File Intelligence")
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Public Identity / Pseudonym", fontWeight = FontWeight.Bold)
                                Text(
                                    "Build name variants, handle candidates, public search queries and profile links.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("First name") },
                            placeholder = { Text("Alex") }
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Last name (optional)") },
                            placeholder = { Text("Martin") }
                        )
                        OutlinedTextField(
                            value = handle,
                            onValueChange = { handle = it.take(65) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            label = { Text("Public pseudo / handle") },
                            placeholder = { Text("alex_dev") }
                        )
                        OutlinedTextField(
                            value = publicEmail,
                            onValueChange = { publicEmail = it.take(254) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Public email (optional, local metadata only)") },
                            placeholder = { Text("contact@example.org") }
                        )
                        Button(
                            onClick = { viewModel.inspectIdentity(firstName, lastName, handle, publicEmail) },
                            enabled = firstName.isNotBlank() || lastName.isNotBlank() || handle.isNotBlank() || publicEmail.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Build public OSINT profile")
                        }
                        Text(
                            "No breach lookup, private-account access, home-address search or phone-owner lookup.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OsintInputCard(
                    title = "Domain Inspector",
                    subtitle = "Parse hostname structure, suffix and IDN form, then open passive public sources such as RDAP, CT logs and Wayback.",
                    icon = Icons.Default.Language,
                    value = domain,
                    placeholder = "example.org",
                    onValueChange = { domain = it.take(253) },
                    onAnalyze = { viewModel.inspectDomain(domain) }
                )
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Live Public Host / Web Surface", fontWeight = FontWeight.Bold)
                                Text(
                                    "Resolve A/AAAA + reverse names, or inspect one public HTTP(S) endpoint for status, TLS certificate and security headers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = liveTarget,
                            onValueChange = { liveTarget = it.take(500) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                            placeholder = { Text("https://example.org") }
                        )
                        Button(
                            onClick = { viewModel.resolveDnsLive(liveTarget) },
                            enabled = liveTarget.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resolve DNS + reverse")
                        }
                        OutlinedButton(
                            onClick = { viewModel.inspectWebSurface(liveTarget) },
                            enabled = liveTarget.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Inspect HTTP + TLS surface")
                        }
                        Text(
                            "Web/TLS inspection refuses targets that resolve only to local, private or special-use addresses. Redirects are reported but not followed automatically.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OsintInputCard(
                    title = "IP / Host Inspector",
                    subtitle = "Classify IPv4/IPv6 scope and attempt a reverse/canonical hostname lookup.",
                    icon = Icons.Default.Router,
                    value = ip,
                    placeholder = "8.8.8.8",
                    onValueChange = { ip = it.take(64) },
                    onAnalyze = { viewModel.inspectIp(ip) }
                )
            }

            item {
                OsintInputCard(
                    title = "URL Inspector",
                    subtitle = "Break down HTTP/HTTPS scheme, host, port, path and query parameter names, with passive discovery links.",
                    icon = Icons.Default.Link,
                    value = url,
                    placeholder = "https://example.org/path",
                    onValueChange = { url = it.take(500) },
                    onAnalyze = { viewModel.inspectUrl(url) }
                )
            }

            if (results.isNotEmpty()) {
                item {
                    Text("RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            items(results) { result ->
                val container = if (result.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                val content = if (result.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = container)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(result.title, fontWeight = FontWeight.Bold, color = content)
                        Text(result.output, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = content)
                        if (result.links.isNotEmpty()) {
                            HorizontalDivider()
                            Text("PUBLIC / PASSIVE LINKS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            result.links.forEach { link ->
                                OutlinedButton(
                                    onClick = { runCatching { uriHandler.openUri(link.url) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(link.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OsintInputCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(placeholder) }
            )
            Button(onClick = onAnalyze, enabled = value.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Analyze")
            }
        }
    }
}
