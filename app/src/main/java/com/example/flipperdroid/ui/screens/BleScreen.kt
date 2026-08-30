package com.example.flipperdroid.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.BleDevice
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.RssiSparkline
import com.example.flipperdroid.ui.components.SignalPill
import com.example.flipperdroid.ui.components.maskMac

@Composable
fun BleScreen(viewModel: AppViewModel) {
    val devices by viewModel.bleDevices.collectAsState()
    val scanning by viewModel.bleScanning.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var filter by remember { mutableStateOf("") }
    var connectableOnly by remember { mutableStateOf(false) }
    var minRssi by remember { mutableFloatStateOf(-100f) }
    var sortByName by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) viewModel.startBleScan()
    }

    fun requestAndStart() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(permissions)
    }

    fun shareCsv() {
        val csv = viewModel.exportBleCsv()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid — export scan BLE")
        }
        context.startActivity(Intent.createChooser(intent, "Partager l'export BLE"))
    }

    val visible = remember(devices, filter, connectableOnly, minRssi, sortByName) {
        devices.asSequence()
            .filter { filter.isBlank() || it.name.contains(filter, true) || it.address.contains(filter, true) ||
                it.serviceUuids.any { uuid -> uuid.contains(filter, true) } ||
                (it.manufacturerName?.contains(filter, true) == true) }
            .filter { !connectableOnly || it.connectable == true }
            .filter { it.rssi >= minRssi.toInt() }
            .sortedWith(if (sortByName) compareBy { it.name.lowercase() } else compareByDescending { it.rssi })
            .toList()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "Bluetooth LE",
                "Scan passif, tri par puissance, fabricant et services. Le scan s'arrête automatiquement après 30 secondes."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = ::requestAndStart,
                    enabled = !scanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scanner")
                }
                OutlinedButton(onClick = viewModel::stopBleScan, enabled = scanning) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
                IconButton(onClick = viewModel::clearBle) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Vider")
                }
                IconButton(onClick = ::shareCsv, enabled = devices.isNotEmpty()) {
                    Icon(Icons.Default.IosShare, contentDescription = "Exporter CSV")
                }
            }
        }
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it.take(80) },
                label = { Text("Filtrer nom / MAC / UUID / fabricant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = connectableOnly,
                            onClick = { connectableOnly = !connectableOnly },
                            label = { Text("Connectables uniquement") }
                        )
                        FilterChip(
                            selected = sortByName,
                            onClick = { sortByName = !sortByName },
                            label = { Text(if (sortByName) "Tri: nom" else "Tri: RSSI") }
                        )
                    }
                    Text("RSSI minimum: ${minRssi.toInt()} dBm", style = MaterialTheme.typography.bodySmall)
                    Slider(value = minRssi, onValueChange = { minRssi = it }, valueRange = -100f..-30f)
                }
            }
        }
        item {
            Text(
                if (scanning) "Scan en cours · ${visible.size} appareil(s)" else "${visible.size} appareil(s)",
                fontWeight = FontWeight.SemiBold
            )
        }
        items(visible, key = { it.address }) { device ->
            BleDeviceCard(device, appSettings.privacyMode)
        }
    }
}

@Composable
private fun BleDeviceCard(device: BleDevice, privacyMode: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(device.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SignalPill(device.rssi)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(maskMac(device.address, privacyMode), style = MaterialTheme.typography.bodySmall)
                if (device.randomAddress) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.SyncAlt, null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Adresse aléatoire", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Text("Fabricant: ${device.manufacturerName ?: "non annoncé"}", style = MaterialTheme.typography.bodySmall)
            device.txPower?.let { Text("Tx power annoncé: $it dBm", style = MaterialTheme.typography.bodySmall) }
            device.connectable?.let { Text("Connectable: ${if (it) "oui" else "non"}", style = MaterialTheme.typography.bodySmall) }
            if (device.packetCount > 1) {
                Text("Paquets reçus: ${device.packetCount}", style = MaterialTheme.typography.bodySmall)
            }
            if (device.rssiHistory.size > 1) {
                Text("Tendance RSSI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RssiSparkline(device.rssiHistory)
            }
            if (device.serviceUuids.isNotEmpty()) {
                Text("Services:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                device.serviceUuids.take(4).forEach { uuid ->
                    Text(uuid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (device.serviceUuids.size > 4) {
                    Text("+${device.serviceUuids.size - 4} autre(s)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
