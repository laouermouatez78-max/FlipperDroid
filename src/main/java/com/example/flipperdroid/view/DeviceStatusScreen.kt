package com.example.flipperdroid.view

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

private data class CapabilityStatus(val name: String, val value: String, val available: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val statuses = remember(refreshTick) {
        val pm = context.packageManager
        val nfc = NfcAdapter.getDefaultAdapter(context)
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bt = btManager?.adapter
        val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val hasBtConnectPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val btEnabled = if (hasBtConnectPermission) runCatching { bt?.isEnabled == true }.getOrDefault(false) else false
        val multiAdvertise = if (hasBtConnectPermission) runCatching { bt?.isMultipleAdvertisementSupported == true }.getOrDefault(false) else false
        val bleAdvertiserPresent = if (hasBtConnectPermission) runCatching { bt?.bluetoothLeAdvertiser != null }.getOrDefault(false) else false

        listOf(
            CapabilityStatus(
                "Android",
                "Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ),
            CapabilityStatus(
                "NFC",
                when {
                    nfc == null -> "No NFC adapter"
                    runCatching { nfc.isEnabled }.getOrDefault(false) -> "Hardware present · enabled"
                    else -> "Hardware present · disabled"
                },
                nfc != null
            ),
            CapabilityStatus(
                "Bluetooth LE scanner",
                when {
                    bt == null -> "Bluetooth adapter unavailable"
                    !pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "BLE hardware not reported"
                    !hasBtConnectPermission -> "BLE present · Nearby devices permission required"
                    !btEnabled -> "BLE present · Bluetooth disabled"
                    else -> "BLE scanner available"
                },
                pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ),
            CapabilityStatus(
                "BLE advertiser",
                when {
                    bt == null -> "Bluetooth adapter unavailable"
                    !hasBtConnectPermission -> "Permission required to verify advertiser"
                    !multiAdvertise -> "Multiple advertisement not supported"
                    !bleAdvertiserPresent -> "Advertiser unavailable"
                    else -> "V4 test beacon supported"
                },
                multiAdvertise && bleAdvertiserPresent
            ),
            CapabilityStatus(
                "Wi‑Fi",
                if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) "Wi‑Fi hardware present" else "Wi‑Fi not reported",
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
            ),
            CapabilityStatus(
                "USB Host",
                if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
                    "Supported · ${runCatching { usb?.deviceList?.size ?: 0 }.getOrDefault(0)} attached device(s)"
                } else {
                    "USB Host not reported"
                },
                pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
            ),
            CapabilityStatus(
                "Infrared emitter",
                if (runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)) "IR transmitter available" else "IR transmitter unavailable",
                runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)
            ),
            CapabilityStatus(
                "Network",
                if (connectivity?.activeNetwork != null) "Active network detected" else "No active network",
                connectivity?.activeNetwork != null
            )
        )
    }

    val readyCount = statuses.count { it.available }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Status · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Compatibility overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$readyCount/${statuses.size} capabilities currently available or reported by this phone.")
                        LinearProgressIndicator(
                            progress = { readyCount.toFloat() / statuses.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Some hardware can be present but disabled or waiting for Android permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            statuses.forEach { status ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (status.available) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (status.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Column(Modifier.weight(1f)) {
                                Text(status.name, fontWeight = FontWeight.Bold)
                                Text(status.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
