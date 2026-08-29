package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.UsbInspectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbInspectorScreen(navController: NavController, viewModel: UsbInspectorViewModel) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.status.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB Inspector · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Android USB Host", fontWeight = FontWeight.Bold)
                            Text(status)
                        }
                    }
                }
            }

            items(devices) { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(device.productName ?: "USB device #${device.deviceId}", fontWeight = FontWeight.Bold)
                        Text("VID:PID  %04X:%04X".format(device.vendorId, device.productId), fontFamily = FontFamily.Monospace)
                        Text("Class ${device.deviceClass} · permission ${if (device.permissionGranted) "granted" else "not granted"}")
                        device.manufacturer?.let { Text("Manufacturer: $it") }
                        device.serialNumber?.let { Text("Serial: $it") }
                        HorizontalDivider()
                        device.interfaces.forEach { intf ->
                            Text("Interface ${intf.id}: class ${intf.interfaceClass}, sub ${intf.subclass}, proto ${intf.protocol}")
                            intf.endpoints.forEach { ep ->
                                Text(
                                    "  EP 0x%02X · dir ${ep.direction} · type ${ep.type} · max ${ep.maxPacketSize}".format(ep.address),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "This screen inspects real USB peripherals connected to your phone. Android USB Host can inspect/control peripherals; stock Android generally cannot impersonate a USB keyboard without special gadget-mode support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
