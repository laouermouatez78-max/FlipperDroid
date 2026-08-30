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
import com.example.flipperdroid.viewmodel.OsintDefenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsintDefenseScreen(navController: NavController, viewModel: OsintDefenseViewModel) {
    val results by viewModel.results.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Domain & URL Defense · V5") },
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
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid V5 Domain & URL Defense Report")
                                    putExtra(Intent.EXTRA_TEXT, viewModel.reportText())
                                }
                                context.startActivity(Intent.createChooser(share, "Share defense report"))
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
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Defensive OSINT", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Local URL-risk heuristics and user-triggered public DNS checks for mail-domain security posture. Scores are indicators, not verdicts.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("URL Risk Heuristics", fontWeight = FontWeight.Bold)
                                Text(
                                    "Analyze the URL locally for IDN/punycode, embedded credentials, suspicious parameter names, encoding, length, ports and other structural signals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it.take(4000) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            placeholder = { Text("https://example.org/path?next=...") }
                        )
                        Button(
                            onClick = { viewModel.analyzeUrlRisk(url) },
                            enabled = url.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze URL locally")
                        }
                        Text(
                            "Query values are not printed. The heuristic score is not a phishing or malware verdict.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Mail Domain Security Posture", fontWeight = FontWeight.Bold)
                                Text(
                                    "Check public DNS signals for MX, SPF, DMARC, MTA-STS, TLS-RPT, CAA and DNSSEC authenticated-data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it.take(253) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("example.org") }
                        )
                        Button(
                            onClick = { viewModel.inspectMailDomain(domain) },
                            enabled = domain.isNotBlank() && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Inspect public mail DNS")
                        }
                    }
                }
            }

            if (results.isNotEmpty()) {
                item {
                    Text("DEFENSE RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
