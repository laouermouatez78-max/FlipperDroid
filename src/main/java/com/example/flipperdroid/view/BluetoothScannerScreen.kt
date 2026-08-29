package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
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
import com.example.flipperdroid.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScannerScreen(navController: NavController, viewModel: BluetoothViewModel) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val status by viewModel.status.collectAsState()
    val connectedAddress by viewModel.connectedAddress.collectAsState()
    val services by viewModel.gattServices.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPermissionState()
        if (viewModel.permissionsGranted.value) viewModel.startScan()
    }

    LaunchedEffect(Unit) { viewModel.initialize(context) }
    DisposableEffect(Unit) { onDispose { viewModel.stopScan() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Explorer · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() }, enabled = permissionsGranted) {
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
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Scan + GATT explorer", fontWeight = FontWeight.Bold)
                        Text("Discover advertisements, then connect to a BLE device you control to enumerate its GATT services and characteristics.")
                    }
                }
            }

            if (!permissionsGranted) {
                Button(onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Bluetooth permissions")
                }
            }

            Text(status)
            if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (connectedAddress != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("GATT connected", fontWeight = FontWeight.Bold)
                                Text(connectedAddress ?: "", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = viewModel::disconnectGatt) {
                                Icon(Icons.Default.LinkOff, contentDescription = "Disconnect")
                            }
                        }
                        if (services.isEmpty()) {
                            Text("Discovering services…")
                        } else {
                            services.forEach { service ->
                                Text(service.uuid, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                service.characteristics.forEach { characteristic ->
                                    val flags = buildList {
                                        if (characteristic.readable) add("R")
                                        if (characteristic.writable) add("W")
                                        if (characteristic.notifiable) add("N")
                                    }.joinToString("/").ifBlank { "-" }
                                    Text("  ${characteristic.uuid} [$flags]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.address }) { device ->
                    BleDeviceCard(device, connectedAddress == device.address) {
                        if (connectedAddress == device.address) viewModel.disconnectGatt() else viewModel.connectGatt(device.address)
                    }
                }
            }
        }
    }
}

@Composable
private fun BleDeviceCard(device: BleDeviceInfo, connected: Boolean, onConnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(device.address, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onConnect, enabled = device.connectable != false) {
                    Icon(if (connected) Icons.Default.LinkOff else Icons.Default.Link, contentDescription = if (connected) "Disconnect" else "Connect")
                }
            }
            Text("RSSI ${device.rssi} dBm")
            device.connectable?.let { Text("Connectable: ${if (it) "yes" else "no"}") }
            if (device.serviceUuids.isNotEmpty()) {
                Text("Advertised services: ${device.serviceUuids.take(4).joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
