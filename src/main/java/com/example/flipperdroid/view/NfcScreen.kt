package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var cloneUid by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "History")
                    }
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(Icons.Default.Save, contentDescription = "Logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Section Lecture
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Card Reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("UID: ${currentTagUid ?: "-"}")
                    Text("Type: ${currentTagType ?: "-"}")
                    if (currentTagDump.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Dump:", fontWeight = FontWeight.Bold)
                        LazyColumn(Modifier.heightIn(max = 200.dp)) {
                            items(currentTagDump.size) { i ->
                                Text(currentTagDump[i], fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                        Button(onClick = { nfcViewModel.onDumpExport() }, Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Default.Save, contentDescription = "Export")
                            Spacer(Modifier.width(4.dp))
                            Text("Export dump")
                        }
                    }
                }
            }
            // Section Clonage
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("UID Cloning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cloneUid,
                        onValueChange = { cloneUid = it },
                        label = { Text("New UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { nfcViewModel.onCloneUid(cloneUid) }, Modifier.padding(top = 8.dp)) {
                        Text("Clone UID to card")
                    }
                }
            }
            // Section Logs
            if (showLogs) {
                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyColumn(Modifier.heightIn(max = 150.dp)) {
                            items(logs.size) { i ->
                                Text(logs[i], fontSize = MaterialTheme.typography.bodySmall.fontSize)
                            }
                        }
                        Button(onClick = { nfcViewModel.clearLogs() }, Modifier.padding(top = 8.dp)) {
                            Text("Clear logs")
                        }
                    }
                }
            }
            // Section Historique
            if (showHistory) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Scan History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyColumn(Modifier.heightIn(max = 150.dp)) {
                            items(scanHistory.size) { i ->
                                val scan = scanHistory[scanHistory.size - 1 - i]
                                Text("${scan.timestamp} - UID: ${scan.uid ?: "-"} - Type: ${scan.type ?: "-"}")
                            }
                        }
                    }
                }
            }
        }
    }
}
