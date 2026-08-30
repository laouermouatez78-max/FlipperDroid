package com.example.flipperdroid.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.BleDevice
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.SignalPill
import com.example.flipperdroid.ui.components.maskMac

@Composable
fun BleScreen(viewModel: AppViewModel) {
    val devices by viewModel.bleDevices.collectAsState()
    val scanning by viewModel.bleScanning.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    var filter by remember { mutableStateOf("") }

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

    val visible = remember(devices, filter) {
        if (filter.isBlank()) devices else devices.filter {
            it.name.contains(filter, true) || it.address.contains(filter, true) ||
                it.serviceUuids.any { uuid -> uuid.contains(filter, true) }
        }
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
            }
        }
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it.take(80) },
                label = { Text("Filtrer nom / MAC / UUID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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
            Text(maskMac(device.address, privacyMode), style = MaterialTheme.typography.bodySmall)
            val manufacturer = device.manufacturerId?.let { "0x%04X".format(it) } ?: "non annoncé"
            Text("Fabricant: $manufacturer", style = MaterialTheme.typography.bodySmall)
            device.txPower?.let { Text("Tx power annoncé: $it dBm", style = MaterialTheme.typography.bodySmall) }
            device.connectable?.let { Text("Connectable: ${if (it) "oui" else "non"}", style = MaterialTheme.typography.bodySmall) }
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
