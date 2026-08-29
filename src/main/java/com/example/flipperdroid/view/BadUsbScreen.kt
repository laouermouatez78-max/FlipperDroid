package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val isUsbHostAvailable by viewModel.isUsbHostAvailable
    val isConnected by viewModel.isConnected
    val currentScript by viewModel.currentScript.collectAsState()
    val status by viewModel.status.collectAsState()
    val previewLog by viewModel.previewLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB HID Lab") },
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
                    Icon(Icons.Default.Usb, contentDescription = null)
                    Column {
                        Text("BadUSB workflow restored", fontWeight = FontWeight.Bold)
                        Text("V3 lets you author, validate and replay scripts in a local simulator. It does not inject keystrokes into another machine.")
                    }
                }
            }

            AssistChip(
                onClick = {},
                label = { Text(if (isUsbHostAvailable) "USB Host detected" else "USB Host unavailable") }
            )

            OutlinedTextField(
                value = currentScript,
                onValueChange = viewModel::updateScript,
                label = { Text("Script / text to simulate") },
                supportingText = { Text("Max 20,000 characters. Simulation only.") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.executeScript() },
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
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isConnected) "End session" else "Start lab")
                }
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)
            Text("Preview log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(previewLog.size) { index ->
                    Text(
                        previewLog[index],
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
