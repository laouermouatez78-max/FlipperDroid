package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.EmvReaderViewModel
import com.example.flipperdroid.viewmodel.NfcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvReaderScreen(
    navController: NavController,
    viewModel: EmvReaderViewModel,
    nfcViewModel: NfcViewModel
) {
    val cardData by viewModel.cardData.collectAsState()
    val isReading by viewModel.isReading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastTag by nfcViewModel.lastTag.collectAsState()
    val lastUid by nfcViewModel.currentTagUid.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMV Metadata Reader") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Column {
                        Text("Privacy mode", fontWeight = FontWeight.Bold)
                        Text("V3 identifies only the contactless application/scheme and AID. PAN, expiry, cardholder name and Track 2 are not read or retained.")
                    }
                }
            }

            if (lastTag != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("NFC tag ready", fontWeight = FontWeight.Bold)
                        Text("UID: ${lastUid ?: "detected"}", fontFamily = FontFamily.Monospace)
                        Button(
                            onClick = { lastTag?.let(viewModel::readCard) },
                            enabled = !isReading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Contactless, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Read EMV metadata")
                        }
                    }
                }
            } else {
                Text(
                    "Present a contactless card once so Android detects the NFC tag, then return here to request metadata reading.",
                    textAlign = TextAlign.Center
                )
            }

            when {
                isReading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text("Reading EMV application metadata…\nKeep the card steady.", textAlign = TextAlign.Center)
                }

                cardData != null -> {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(56.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Detected application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Scheme: ${cardData?.cardType ?: "Unknown"}")
                            Text(
                                "AID: ${cardData?.aid ?: "Unknown"}",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Contactless: ${if (cardData?.contactless == true) "Yes" else "Unknown"}")
                        }
                    }
                    Button(onClick = { viewModel.clearData() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear result")
                    }
                }

                error != null -> {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = { viewModel.clearData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset")
                    }
                }
            }
        }
    }
}
