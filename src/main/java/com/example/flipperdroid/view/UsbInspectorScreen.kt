package com.example.flipperdroid.view

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.UsbInspectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbInspectorScreen(navController: NavController, viewModel: UsbInspectorViewModel) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.status.collectAsState()
    val usbHostSupported by viewModel.usbHostSupported.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB Inspector · V5") },
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
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Icon(Icons.Default.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Android USB Host", fontWeight = FontWeight.Bold)
                            Text(
                                if (usbHostSupported) "USB Host supported" else "USB Host not reported by this phone",
                                color = if (usbHostSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(if (usbHostSupported) Icons.Default.UsbOff else Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (usbHostSupported) "No USB peripheral detected" else "USB Host unavailable", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(
                                if (usbHostSupported) "Connect a USB device using an OTG adapter/cable, then tap Refresh."
                                else "Real USB inspection needs Android USB Host support.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (usbHostSupported) {
                                OutlinedButton(onClick = viewModel::refresh) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }

            items(devices, key = { it.deviceId }) { device ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(device.productName ?: "USB device #${device.deviceId}", fontWeight = FontWeight.Bold)
                                Text("VID:PID  %04X:%04X".format(device.vendorId, device.productId), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(if (device.permissionGranted) "ACCESS OK" else "LOCKED") },
                                leadingIcon = { Icon(if (device.permissionGranted) Icons.Default.CheckCircle else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Text("Device class: ${UsbInspectorViewModel.classLabel(device.deviceClass)} · ${device.interfaces.size} interface(s)")
                        device.manufacturer?.let { Text("Manufacturer: $it") }
                        UsbInspectorViewModel.maskSerial(device.serialNumber)?.let {
                            Text("Serial: $it (masked)", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }

                        if (!device.permissionGranted) {
                            Button(onClick = { viewModel.requestPermission(device.deviceId) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Grant USB access")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.testOpenConnection(device.deviceId) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Cable, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Run non-destructive open/close test")
                            }
                        }

                        HorizontalDivider()
                        if (device.interfaces.isEmpty()) {
                            Text("No USB interface descriptor exposed.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            device.interfaces.forEach { intf ->
                                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            "Interface ${intf.id} · ${UsbInspectorViewModel.classLabel(intf.interfaceClass)} · sub ${intf.subclass} · proto ${intf.protocol}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (intf.endpoints.isEmpty()) {
                                            Text("No endpoints", style = MaterialTheme.typography.labelSmall)
                                        } else {
                                            intf.endpoints.forEach { ep ->
                                                Text(
                                                    "EP 0x%02X · %s · %s · max %d B".format(
                                                        ep.address,
                                                        UsbInspectorViewModel.endpointDirectionLabel(ep.direction),
                                                        UsbInspectorViewModel.endpointTypeLabel(ep.type),
                                                        ep.maxPacketSize
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "V5 reads USB descriptors and can perform a non-destructive open/close test. Device serial numbers are masked in the UI by default.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
