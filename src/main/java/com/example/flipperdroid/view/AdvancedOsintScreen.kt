package com.example.flipperdroid.view

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.AdvancedOsintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOsintScreen(navController: NavController, viewModel: AdvancedOsintViewModel) {
    val results by viewModel.results.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var dnsTarget by remember { mutableStateOf("") }
    var rdapTarget by remember { mutableStateOf("") }
    var domainTarget by remember { mutableStateOf("") }
    var indicatorText by remember { mutableStateOf("") }
    var emailHeaders by remember { mutableStateOf("") }
    var fingerprintText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced OSINT · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(viewModel.reportText())) },
                        enabled = results.isNotEmpty()
                    ) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy report") }
                    IconButton(
                        onClick = {
                            if (results.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid V5 OSINT Report")
                                    putExtra(Intent.EXTRA_TEXT, viewModel.reportText())
                                }
                                context.startActivity(Intent.createChooser(intent, "Share OSINT report"))
                            }
                        },
                        enabled = results.isNotEmpty()
                    ) { Icon(Icons.Default.Share, contentDescription = "Share report") }
                    IconButton(onClick = viewModel::clear, enabled = results.isNotEmpty()) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
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
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.ManageSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Advanced public intelligence", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "User-triggered public DNS/RDAP checks plus local parsers for indicators, email headers and domain similarity. No credential access, breach databases or private-account collection.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                AdvancedInputCard(
                    title = "DNS Record Intelligence",
                    subtitle = "Query A, AAAA, MX, NS, TXT and CAA records through Google Public DNS over HTTPS.",
                    icon = Icons.Default.Dns,
                    value = dnsTarget,
                    placeholder = "example.org",
                    singleLine = true,
                    onValueChange = { dnsTarget = it.take(500) },
                    button = "Query DNS records",
                    enabled = dnsTarget.isNotBlank() && !busy,
                    onRun = { viewModel.queryDnsRecords(dnsTarget) }
                )
            }

            item {
                AdvancedInputCard(
                    title = "RDAP Registry Intelligence",
                    subtitle = "Read public registration/allocation metadata for a domain or public IP.",
                    icon = Icons.Default.AccountTree,
                    value = rdapTarget,
                    placeholder = "example.org or 8.8.8.8",
                    singleLine = true,
                    onValueChange = { rdapTarget = it.take(500) },
                    button = "Query public RDAP",
                    enabled = rdapTarget.isNotBlank() && !busy,
                    onRun = { viewModel.queryRdap(rdapTarget) }
                )
            }

            item {
                AdvancedInputCard(
                    title = "Domain Similarity / Typosquatting",
                    subtitle = "Generate likely look-alike domain variants locally without checking registration or contacting them.",
                    icon = Icons.Default.DomainVerification,
                    value = domainTarget,
                    placeholder = "example.org",
                    singleLine = true,
                    onValueChange = { domainTarget = it.take(253) },
                    button = "Generate similarity candidates",
                    enabled = domainTarget.isNotBlank(),
                    onRun = { viewModel.generateDomainVariants(domainTarget) }
                )
            }

            item {
                AdvancedInputCard(
                    title = "IOC / Artifact Extractor",
                    subtitle = "Parse pasted logs or notes locally for URLs, domains, IPv4, emails and common hashes.",
                    icon = Icons.Default.FilterAlt,
                    value = indicatorText,
                    placeholder = "Paste logs, incident notes, headers or text…",
                    singleLine = false,
                    onValueChange = { indicatorText = it.take(100_000) },
                    button = "Extract indicators locally",
                    enabled = indicatorText.isNotBlank(),
                    onRun = { viewModel.extractIndicators(indicatorText) }
                )
            }

            item {
                AdvancedInputCard(
                    title = "Email Header Analyzer",
                    subtitle = "Inspect raw message headers locally: Received hops, From/Reply-To/Return-Path, Message-ID and SPF/DKIM/DMARC hints.",
                    icon = Icons.Default.MarkEmailRead,
                    value = emailHeaders,
                    placeholder = "Paste raw email headers…",
                    singleLine = false,
                    onValueChange = { emailHeaders = it.take(100_000) },
                    button = "Analyze headers locally",
                    enabled = emailHeaders.isNotBlank(),
                    onRun = { viewModel.analyzeEmailHeaders(emailHeaders) }
                )
            }

            item {
                AdvancedInputCard(
                    title = "Text Fingerprint",
                    subtitle = "Create local SHA-256/SHA-1 fingerprints and basic structural metadata for copied text.",
                    icon = Icons.Default.Fingerprint,
                    value = fingerprintText,
                    placeholder = "Text to fingerprint…",
                    singleLine = false,
                    onValueChange = { fingerprintText = it.take(100_000) },
                    button = "Fingerprint locally",
                    enabled = fingerprintText.isNotBlank(),
                    onRun = { viewModel.fingerprintText(fingerprintText) }
                )
            }

            if (results.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ADVANCED RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("${results.size} result(s)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(results) { result ->
                val background = if (result.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                val foreground = if (result.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = background)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(result.title, fontWeight = FontWeight.Bold, color = foreground)
                        SelectionContainer {
                            Text(result.output, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = foreground)
                        }
                        TextButton(onClick = { clipboard.setText(AnnotatedString(result.output)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy result")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedInputCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onValueChange: (String) -> Unit,
    button: String,
    enabled: Boolean,
    onRun: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
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
                modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.heightIn(min = 130.dp)),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 5,
                maxLines = if (singleLine) 1 else 12,
                placeholder = { Text(placeholder) }
            )
            Button(onClick = onRun, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(button)
            }
        }
    }
}
