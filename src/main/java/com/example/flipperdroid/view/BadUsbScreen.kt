package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.BadUsbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadUsbScreen(
    navController: NavController,
    viewModel: BadUsbViewModel
) {
    val context = LocalContext.current
    val isUsbHostAvailable by viewModel.isUsbHostAvailable
    val isConnected by viewModel.isConnected
    val currentScript by viewModel.currentScript.collectAsState()
    val status by viewModel.status.collectAsState()
    val previewLog by viewModel.previewLog.collectAsState()

    LaunchedEffect(Unit) { viewModel.initialize(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB HID Lab · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(28.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Local HID script simulator", fontWeight = FontWeight.Bold)
                        Text(
                            "Author, validate and replay HID-style steps locally. V4 never injects keystrokes into another computer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (isUsbHostAvailable) "USB Host detected" else "USB Host unavailable") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(if (isConnected) "SESSION ACTIVE" else "LAB READY") }
                )
            }

            OutlinedTextField(
                value = currentScript,
                onValueChange = viewModel::updateScript,
                label = { Text("Script / text to simulate") },
                supportingText = { Text("Local preview only · max 20,000 characters") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 220.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::executeScript,
                    enabled = currentScript.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simulate")
                }
                OutlinedButton(
                    onClick = { if (isConnected) viewModel.disconnect() else viewModel.startLabSession() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isConnected) Icons.Default.Stop else Icons.Default.Science, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isConnected) "Stop" else "Start lab")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(status, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            Text("Simulation log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (previewLog.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Run a simulation to see each step here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(previewLog.size) { index ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                previewLog[index],
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
