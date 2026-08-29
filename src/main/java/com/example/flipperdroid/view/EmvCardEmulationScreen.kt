package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.EmvCardEmulationViewModel
import com.example.flipperdroid.viewmodel.NfcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvCardEmulationScreen(
    navController: NavController,
    viewModel: EmvCardEmulationViewModel,
    nfcViewModel: NfcViewModel
) {
    val isActive by viewModel.isEmulationActive.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val readerStatus by viewModel.readerStatus.collectAsState()
    val lastTag by nfcViewModel.lastTag.collectAsState()
    val lastUid by nfcViewModel.currentTagUid.collectAsState()
    val lastType by nfcViewModel.currentTagType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC APDU Lab · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearTranscript) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear transcript")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Contactless, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Real phone-to-phone APDU", fontWeight = FontWeight.Bold)
                            Text(
                                "One Android phone emulates a custom FlipperDroid HCE card. A second phone sends real ISO-DEP APDUs: SELECT, PING and ECHO.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PHONE A · TEST CARD", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("FlipperDroid HCE card", fontWeight = FontWeight.Bold)
                                Text(
                                    if (isActive) "Active · ready for the second phone" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(checked = isActive, onCheckedChange = viewModel::setEmulationActive)
                        }
                        Text(
                            "AID: F0010203040506 · custom lab protocol only. No payment credentials are emulated.",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PHONE B · READER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Tap Phone A with NFC", fontWeight = FontWeight.Bold)
                                Text("UID: ${lastUid ?: "waiting"}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                Text(lastType ?: "No ISO-DEP target captured", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(readerStatus, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { viewModel.runReaderTest(lastTag) },
                            enabled = lastTag != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Run real SELECT + PING + ECHO")
                        }
                    }
                }
            }

            item {
                Text("APDU transcript", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "A passing test ends with REAL APDU TEST PASSED. The log below shows actual reader/card frames.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (transcript.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("No APDU frames yet", modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(transcript.size) { index ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            transcript[index],
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
