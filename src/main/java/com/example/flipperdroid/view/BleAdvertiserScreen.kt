package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.BleAdvertiserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleAdvertiserScreen(navController: NavController, viewModel: BleAdvertiserViewModel) {
    val active by viewModel.isAdvertising.collectAsState()
    val status by viewModel.status.collectAsState()
    val name by viewModel.localName.collectAsState()
    var permissionsGranted by remember { mutableStateOf(viewModel.permissionsGranted()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsGranted = viewModel.permissionsGranted()
        if (permissionsGranted) viewModel.start()
    }

    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Test Beacon · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("One normal BLE advertisement", fontWeight = FontWeight.Bold)
                        Text(
                            "Broadcast a FlipperDroid V5 test UUID so a second device you control can verify BLE discovery. The beacon auto-stops after 60 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = viewModel::updateName,
                label = { Text("Temporary BLE device name") },
                supportingText = { Text("V5 restores the original Bluetooth name when the beacon stops.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !active
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    leadingIcon = if (permissionsGranted) { { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    label = { Text(if (permissionsGranted) "Permissions OK" else "Permissions required") }
                )
                AssistChip(onClick = {}, label = { Text(if (viewModel.canAdvertise()) "Advertiser supported" else "Unsupported") })
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(status, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            if (!permissionsGranted) {
                OutlinedButton(onClick = { launcher.launch(viewModel.requiredPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Bluetooth permissions")
                }
            }

            Button(
                onClick = {
                    if (active) viewModel.stop()
                    else if (permissionsGranted) viewModel.start()
                    else launcher.launch(viewModel.requiredPermissions())
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = active || viewModel.canAdvertise()
            ) {
                Icon(if (active) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (active) "Stop beacon" else "Start 60-second test beacon")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How to test", fontWeight = FontWeight.Bold)
                    Text("1. Start the V5 beacon on this phone.")
                    Text("2. Open BLE Explorer on a second phone you control.")
                    Text("3. Scan for the temporary name / V5 service advertisement.")
                    Text("4. The source phone stops advertising automatically after one minute.")
                }
            }
        }
    }
}
