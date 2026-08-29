package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.BleDeviceInfo
import com.example.flipperdroid.viewmodel.BluetoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScannerScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val status by viewModel.status.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionState()
        if (viewModel.permissionsGranted.value) viewModel.startScan()
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() },
                        enabled = permissionsGranted
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                    Column {
                        Text("Passive BLE inventory", fontWeight = FontWeight.Bold)
                        Text("Shows advertisements visible to this phone. No pairing, connection or payload injection is performed.")
                    }
                }
            }

            if (!permissionsGranted) {
                Button(onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) }) {
                    Text("Grant Bluetooth permission")
                }
            }

            Text(status)
            if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.address }) { device ->
                    BleDeviceCard(device)
                }
            }
        }
    }
}

@Composable
private fun BleDeviceCard(device: BleDeviceInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(device.address, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text("RSSI ${device.rssi} dBm")
            device.connectable?.let { Text("Connectable: ${if (it) "yes" else "no"}") }
            if (device.serviceUuids.isNotEmpty()) {
                Text("Services: ${device.serviceUuids.take(4).joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
