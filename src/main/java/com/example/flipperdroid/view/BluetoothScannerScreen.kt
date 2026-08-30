package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.*
import java.util.Locale

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
    val isConnecting by viewModel.isConnecting.collectAsState()
    val elevatedCount = devices.count { it.trafficLevel == BleTrafficLevel.ELEVATED }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPermissionState()
        if (viewModel.permissionsGranted.value) viewModel.startScan()
    }

    LaunchedEffect(Unit) { viewModel.initialize(context) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScan()
            viewModel.disconnectGatt()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Explorer · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() },
                        enabled = permissionsGranted && !isConnecting
                    ) {
                        Icon(if (isScanning) Icons.Default.Stop else Icons.Default.Refresh, contentDescription = if (isScanning) "Stop scan" else "Start scan")
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
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Advertisement decoder + GATT explorer", fontWeight = FontWeight.Bold)
                        Text(
                            "Passively decodes BLE advertisements. GATT metadata discovery is explicit, times out after 12 seconds, and should be used only with owned/authorized devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!permissionsGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bluetooth permission required", fontWeight = FontWeight.Bold)
                        Text("Android needs Nearby devices permission for BLE scan and GATT access.")
                        Button(onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant Bluetooth permissions")
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isScanning || isConnecting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Icon(if (connectedAddress != null) Icons.Default.Link else Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (devices.isNotEmpty()) AssistChip(onClick = {}, label = { Text("${devices.size}") })
                }
            }

            if (elevatedCount > 0) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null)
                        Column(Modifier.weight(1f)) {
                            Text("Elevated BLE advertisement rate observed", fontWeight = FontWeight.Bold)
                            Text(
                                "$elevatedCount device(s) crossed the local rate threshold. This is an observation, not proof of malicious BLE spam; legitimate beacons can advertise frequently.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (connectedAddress != null) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("GATT connected", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(connectedAddress ?: "", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = viewModel::disconnectGatt) { Icon(Icons.Default.LinkOff, contentDescription = "Disconnect") }
                        }

                        if (services.isEmpty()) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Discovering services…", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("${services.size} service(s)", style = MaterialTheme.typography.labelLarge)
                            services.take(20).forEach { service ->
                                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(service.uuid, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        service.characteristics.take(30).forEach { characteristic ->
                                            val flags = buildList {
                                                if (characteristic.readable) add("R")
                                                if (characteristic.writable) add("W")
                                                if (characteristic.notifiable) add("N")
                                            }.joinToString("/").ifBlank { "-" }
                                            Text("${characteristic.uuid} [$flags]", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (isScanning) "Scanning for BLE devices…" else "No BLE devices listed yet", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Text("Scans stop automatically after about 12 seconds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (permissionsGranted && !isScanning && !isConnecting) Button(onClick = viewModel::startScan) { Text("Start scan") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(devices, key = { it.address }) { device ->
                        BleDeviceCard(
                            device = device,
                            connected = connectedAddress == device.address,
                            connectEnabled = !isConnecting || connectedAddress == device.address
                        ) {
                            if (connectedAddress == device.address) viewModel.disconnectGatt() else viewModel.connectGatt(device.address)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BleDeviceCard(device: BleDeviceInfo, connected: Boolean, connectEnabled: Boolean, onConnect: () -> Unit) {
    var expanded by remember(device.address) { mutableStateOf(false) }
    val rateText = String.format(Locale.US, "%.1f", device.advertisementsPerSecond)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(device.address, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalIconButton(onClick = onConnect, enabled = device.connectable != false && connectEnabled) {
                    Icon(if (connected) Icons.Default.LinkOff else Icons.Default.Link, contentDescription = if (connected) "Disconnect" else "Connect")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${device.rssi} dBm") })
                AssistChip(onClick = {}, label = { Text("$rateText adv/s") })
                device.connectable?.let { AssistChip(onClick = {}, label = { Text(if (it) "Connectable" else "Broadcast only") }) }
            }

            if (device.trafficLevel == BleTrafficLevel.ELEVATED) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Elevated observed advertisement rate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (device.serviceUuids.isNotEmpty()) {
                Text("Advertised services: ${device.serviceUuids.take(4).joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            TextButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (expanded) "Hide packet details" else "Show packet details")
            }

            if (expanded) {
                HorizontalDivider()
                Text("Observed ${device.seenCount} packet(s)", style = MaterialTheme.typography.labelMedium)
                device.txPower?.let { Text("Advertised TX power: $it dBm", style = MaterialTheme.typography.bodySmall) }

                if (device.manufacturerData.isNotEmpty()) {
                    Text("Manufacturer data", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    device.manufacturerData.forEach { field ->
                        Text("0x${field.companyId.toString(16).uppercase().padStart(4, '0')}: ${field.dataHex.ifBlank { "<empty>" }}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (device.serviceData.isNotEmpty()) {
                    Text("Service data", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    device.serviceData.forEach { field ->
                        Text("${field.uuid}: ${field.dataHex.ifBlank { "<empty>" }}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Text("Raw advertisement", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                Text(device.rawAdvertisementHex.ifBlank { "No raw bytes exposed by Android" }, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
