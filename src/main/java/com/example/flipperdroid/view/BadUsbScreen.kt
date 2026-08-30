package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import com.example.flipperdroid.viewmodel.UsbInspectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadUsbScreen(
    navController: NavController,
    viewModel: BadUsbViewModel,
    usbViewModel: UsbInspectorViewModel
) {
    val context = LocalContext.current
    val currentScript by viewModel.currentScript.collectAsState()
    val previewLog by viewModel.previewLog.collectAsState()
    val labStatus by viewModel.status.collectAsState()
    val simulating by viewModel.isSimulating.collectAsState()
    val devices by usbViewModel.devices.collectAsState()
    val usbStatus by usbViewModel.status.collectAsState()
    val usbHostSupported by usbViewModel.usbHostSupported.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        usbViewModel.refresh()
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopSimulation() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USB OTG Lab · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = usbViewModel::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh USB") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("USB Host test + local HID parser", fontWeight = FontWeight.Bold)
                            Text(
                                "V5 can request Android permission and perform a real non-destructive USB open/close test. HID-style scripts are previewed only on-screen and never injected as keystrokes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REAL USB TEST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(if (usbHostSupported) "USB Host supported" else "USB Host not reported by this phone", fontWeight = FontWeight.Bold)
                        Text(usbStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    OutlinedButton(onClick = usbViewModel::refresh, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh connected USB devices")
                    }
                }
            } else {
                items(devices.size) { index ->
                    val device = devices[index]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(device.productName ?: "USB device #${device.deviceId}", fontWeight = FontWeight.Bold)
                            Text(
                                "VID:PID %04X:%04X · %s".format(device.vendorId, device.productId, UsbInspectorViewModel.classLabel(device.deviceClass)),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Interfaces: ${device.interfaces.size} · permission: ${if (device.permissionGranted) "granted" else "required"}")
                            if (!device.permissionGranted) {
                                OutlinedButton(onClick = { usbViewModel.requestPermission(device.deviceId) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Grant Android USB access")
                                }
                            } else {
                                Button(onClick = { usbViewModel.testOpenConnection(device.deviceId) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Run real open/close test")
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("HID SCRIPT PREVIEW", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Stock Android USB Host does not universally turn a phone into a USB keyboard. This parser is deliberately local-only and capped at 200 preview steps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (simulating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(labStatus, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = currentScript,
                    onValueChange = viewModel::updateScript,
                    label = { Text("HID-style script / text") },
                    supportingText = { Text("Local validation only · ${currentScript.length}/20,000 characters") },
                    enabled = !simulating,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 220.dp)
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (simulating) viewModel.stopSimulation() else viewModel.executeScript() },
                        enabled = simulating || currentScript.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (simulating) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (simulating) "Stop preview" else "Preview locally")
                    }
                    OutlinedButton(onClick = viewModel::clearPreview, enabled = previewLog.isNotEmpty() && !simulating, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear log")
                    }
                }
            }

            item {
                Text("Parser log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 220.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (previewLog.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No parser events yet") }
                    } else {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            previewLog.takeLast(14).forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }
            }
        }
    }
}
