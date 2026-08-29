package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.EmvCardEmulationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvCardEmulationScreen(
    navController: NavController,
    viewModel: EmvCardEmulationViewModel
) {
    val isActive by viewModel.isEmulationActive.collectAsState()
    val selectedProfile by viewModel.selectedCardType.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMV APDU Lab") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearTranscript() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear transcript")
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Contactless, contentDescription = null)
                    Column {
                        Text("Synthetic EMV simulation", fontWeight = FontWeight.Bold)
                        Text("No real PAN, expiry, Track 2 or payment credential is used. V3 simulates an ISO-DEP/APDU exchange entirely inside the app.")
                    }
                }
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !isActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedProfile)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    viewModel.profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile) },
                            onClick = {
                                viewModel.selectProfile(profile)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Lab session", fontWeight = FontWeight.Bold)
                    Text(if (isActive) "Running synthetic APDU flow" else "Stopped")
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = viewModel::setEmulationActive
                )
            }

            Text("APDU transcript", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(transcript.size) { index ->
                    Text(
                        transcript[index],
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
