package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Public-information analysis", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Analyze public names, pseudonyms, domains, IP addresses and URLs. Identity tools generate candidate public-profile links and search queries without claiming that accounts belong to the same person.",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                    subtitle = "Parse hostname structure, suffix and IDN form.",
                    icon = Icons.Default.Language,
                    value = domain,
                    placeholder = "example.org",
                    onValueChange = { domain = it.take(253) },
                    onAnalyze = { viewModel.inspectDomain(domain) }
                )
            }

            item {
                OsintInputCard(
                    title = "IP / Host Inspector",
                    subtitle = "Classify IPv4/IPv6 scope: public, private, loopback, link-local or multicast.",
                    icon = Icons.Default.Router,
                    value = ip,
                    placeholder = "192.168.1.1",
                    onValueChange = { ip = it.take(64) },
                    onAnalyze = { viewModel.inspectIp(ip) }
                )
            }

            item {
                OsintInputCard(
                    title = "URL Inspector",
                    subtitle = "Break down HTTP/HTTPS scheme, host, port, path and risky embedded user-info.",
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
                            Text("PUBLIC LINKS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
