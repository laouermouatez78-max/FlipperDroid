package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.components.HeroBanner

@Composable
fun UsbScreen(viewModel: AppViewModel) {
    val devices by viewModel.usb.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "USB Inspector",
                "Inventaire local des périphériques USB/OTG: VID/PID, classes, interfaces et endpoints. Aucun script HID n'est exécuté."
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${devices.size} périphérique(s)", fontWeight = FontWeight.Bold)
                IconButton(onClick = viewModel::refreshUsb) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                }
            }
        }
        if (devices.isEmpty()) {
            item { Text("Aucun périphérique USB détecté.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(devices, key = { it.deviceId }) { device ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(device.product ?: device.name, fontWeight = FontWeight.Bold)
                    device.manufacturer?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    device.knownDeviceLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("VID:PID %04X:%04X".format(device.vendorId, device.productId))
                    Text("Classe ${device.deviceClass} · ${device.interfaceCount} interface(s)")
                    if (device.endpoints.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        device.endpoints.take(12).forEach { ep ->
                            Text(
                                "IF ${ep.interfaceId} · ${ep.direction} · ${ep.type} · EP 0x%02X · ${ep.maxPacketSize} B".format(ep.endpointAddress),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
