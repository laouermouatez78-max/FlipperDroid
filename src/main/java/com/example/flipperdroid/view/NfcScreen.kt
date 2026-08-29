package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val ndefSummary by nfcViewModel.ndefSummary.collectAsState()
    val currentTagDump by nfcViewModel.currentTagDump.collectAsState()
    val scanHistory by nfcViewModel.scanHistory.collectAsState()
    val logs by nfcViewModel.logs.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var confirmMemoryRead by remember { mutableStateOf(false) }

    if (confirmMemoryRead) {
        AlertDialog(
            onDismissRequest = { confirmMemoryRead = false },
            title = { Text("Read MIFARE memory?") },
            text = { Text("Only continue for a tag you own or are explicitly authorized to inspect. V3 will try the standard default key and show readable blocks.") },
            confirmButton = {
                Button(onClick = {
                    confirmMemoryRead = false
                    nfcViewModel.readMifareDump()
                }) { Text("Read authorized tag") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMemoryRead = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC / RFID · V3") },
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Current tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("UID: ${currentTagUid ?: "Waiting for a tag…"}", fontFamily = FontFamily.Monospace)
                        Text("Technologies: ${currentTagType ?: "-"}")
                        Text("V3 scans metadata first; memory is read only after your explicit request.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (ndefSummary.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("NDEF metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            ndefSummary.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            if (currentTagUid != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { confirmMemoryRead = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Memory, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Read memory")
                        }
                        OutlinedButton(onClick = { nfcViewModel.onDumpExport() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }
                }
            }

            if (currentTagDump.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MIFARE memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { nfcViewModel.clearDump() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear dump")
                                }
                            }
                            currentTagDump.forEachIndexed { index, line ->
                                Text("$index: $line", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
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
                            if (logs.isEmpty()) Text("No logs yet.")
                            else logs.takeLast(50).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            if (showHistory) {
                item { Text("Scan history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
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
        }
    }
}
