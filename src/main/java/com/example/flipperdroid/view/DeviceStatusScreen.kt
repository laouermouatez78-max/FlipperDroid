package com.example.flipperdroid.view

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class CapabilityStatus(val name: String, val value: String, val available: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(navController: NavController) {
    val context = LocalContext.current
    val statuses = remember {
        val pm = context.packageManager
        val nfc = NfcAdapter.getDefaultAdapter(context)
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bt = btManager?.adapter
        val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val bleAdvertiser = runCatching { bt?.bluetoothLeAdvertiser }.getOrNull()

        listOf(
            CapabilityStatus("Android", "Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N),
            CapabilityStatus(
                "NFC",
                when {
                    nfc == null -> "No NFC adapter"
                    nfc.isEnabled -> "Hardware present · enabled"
                    else -> "Hardware present · disabled"
                },
                nfc != null
            ),
            CapabilityStatus(
                "Bluetooth LE scanner",
                when {
                    bt == null -> "Bluetooth adapter unavailable"
                    !bt.isEnabled -> "Bluetooth present · disabled"
                    pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "BLE scanner available"
                    else -> "BLE not reported"
                },
                pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ),
            CapabilityStatus(
                "BLE advertiser",
                when {
                    bt == null -> "Bluetooth adapter unavailable"
                    !bt.isMultipleAdvertisementSupported -> "Multiple advertisement not supported"
                    bleAdvertiser == null -> "Advertiser unavailable"
                    else -> "V4 test advertising supported"
                },
                bt?.isMultipleAdvertisementSupported == true && bleAdvertiser != null
            ),
            CapabilityStatus("Wi‑Fi", if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) "Wi‑Fi hardware present" else "Wi‑Fi not reported", pm.hasSystemFeature(PackageManager.FEATURE_WIFI)),
            CapabilityStatus(
                "USB Host",
                if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) "Supported · ${usb?.deviceList?.size ?: 0} attached device(s)" else "USB Host not reported",
                pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
            ),
            CapabilityStatus("Infrared emitter", if (ir?.hasIrEmitter() == true) "IR transmitter available" else "IR transmitter unavailable", ir?.hasIrEmitter() == true),
            CapabilityStatus("Network", if (connectivity?.activeNetwork != null) "Active network detected" else "No active network", connectivity?.activeNetwork != null)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Status · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Hardware compatibility check for V4 active tools. A green item means the phone reports the hardware capability; permissions may still need to be granted.")
            }
            statuses.forEach { status ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                            Column {
                                Text(status.name, fontWeight = FontWeight.Bold)
                                Text(status.value, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
