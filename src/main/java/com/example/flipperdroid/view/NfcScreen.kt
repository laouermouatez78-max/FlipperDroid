package com.example.flipperdroid.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.NfcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(
    navController: NavController,
    nfcViewModel: NfcViewModel
) {
    val currentTagUid by nfcViewModel.currentTagUid.collectAsState()
    val currentTagType by nfcViewModel.currentTagType.collectAsState()
    val currentTagDump by nfcViewModel.currentTagDump.collectAsState()
    val scanHistory by nfcViewModel.scanHistory.collectAsState()
    val logs by nfcViewModel.logs.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Reader · V2") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(Icons.Default.Save, contentDescription = "Logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Current tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("UID: ${currentTagUid ?: "Waiting for a tag…"}", fontFamily = FontFamily.Monospace)
                        Text("Type: ${currentTagType ?: "-"}")
                    }
                }
            }

            if (currentTagDump.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tag data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            currentTagDump.forEach { line ->
                                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { nfcViewModel.onDumpExport() }) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export scan")
                            }
                        }
                    }
                }
            }

            if (showLogs) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { nfcViewModel.clearLogs() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                                }
                            }
                            if (logs.isEmpty()) {
                                Text("No logs yet.")
                            } else {
                                logs.takeLast(50).forEach { log ->
                                    Text(log, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            if (showHistory) {
                item {
                    Text("Scan history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (scanHistory.isEmpty()) {
                    item { Text("No scans yet.") }
                } else {
                    items(scanHistory.asReversed()) { scan ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(scan.timestamp, style = MaterialTheme.typography.labelMedium)
                                Text("UID: ${scan.uid ?: "-"}", fontFamily = FontFamily.Monospace)
                                Text("Type: ${scan.type ?: "-"}")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "V2 NFC is intentionally limited to reading, inspection, history and export. Use only with tags you own or are authorized to inspect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
