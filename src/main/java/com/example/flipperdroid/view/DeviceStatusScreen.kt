package com.example.flipperdroid.view

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

private enum class CapabilityLevel { READY, ACTION_REQUIRED, UNAVAILABLE }
private data class CapabilityStatus(val name: String, val value: String, val level: CapabilityLevel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(navController: NavController) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val statuses = remember(refreshTick) { buildCapabilityStatuses(context) }
    val readyCount = statuses.count { it.level == CapabilityLevel.READY }
    val attentionCount = statuses.count { it.level == CapabilityLevel.ACTION_REQUIRED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Status · V5") },
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Compatibility & readiness", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$readyCount/${statuses.size} ready now · $attentionCount need a setting or permission")
                        LinearProgressIndicator(
                            progress = { readyCount.toFloat() / statuses.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Supported hardware can still require Android permissions, Location services or a radio to be enabled.",
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
                            val icon = when (status.level) {
                                CapabilityLevel.READY -> Icons.Default.CheckCircle
                                CapabilityLevel.ACTION_REQUIRED -> Icons.Default.WarningAmber
                                CapabilityLevel.UNAVAILABLE -> Icons.Default.ErrorOutline
                            }
                            val tint = when (status.level) {
                                CapabilityLevel.READY -> MaterialTheme.colorScheme.primary
                                CapabilityLevel.ACTION_REQUIRED -> MaterialTheme.colorScheme.tertiary
                                CapabilityLevel.UNAVAILABLE -> MaterialTheme.colorScheme.error
                            }
                            Icon(icon, contentDescription = null, tint = tint)
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(status.name, fontWeight = FontWeight.Bold)
                                    AssistChip(onClick = {}, label = {
                                        Text(
                                            when (status.level) {
                                                CapabilityLevel.READY -> "READY"
                                                CapabilityLevel.ACTION_REQUIRED -> "ACTION"
                                                CapabilityLevel.UNAVAILABLE -> "NO"
                                            }
                                        )
                                    })
                                }
                                Text(status.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildCapabilityStatuses(context: Context): List<CapabilityStatus> {
    val pm = context.packageManager
    val nfc = NfcAdapter.getDefaultAdapter(context)
    val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bt = btManager?.adapter
    val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    val location = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun granted(permission: String): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    val btConnectGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(Manifest.permission.BLUETOOTH_CONNECT)
    val btScanGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(Manifest.permission.BLUETOOTH_SCAN)
    val btAdvertiseGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(Manifest.permission.BLUETOOTH_ADVERTISE)
    val fineLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || granted(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationEnabled = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) location?.isLocationEnabled == true
        else {
            @Suppress("DEPRECATION")
            location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    }.getOrDefault(false)
    val btEnabled = if (btConnectGranted) runCatching { bt?.isEnabled == true }.getOrDefault(false) else false
    val bleAdvertiserPresent = if (btConnectGranted) runCatching { bt?.bluetoothLeAdvertiser != null }.getOrDefault(false) else false
    val multiAdvertise = if (btConnectGranted) runCatching { bt?.isMultipleAdvertisementSupported == true }.getOrDefault(false) else false
    val active = connectivity?.activeNetwork
    val caps = active?.let { connectivity.getNetworkCapabilities(it) }
    val transports = buildList {
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("Wi‑Fi")
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("Cellular")
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("Ethernet")
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("VPN")
    }

    return listOf(
        CapabilityStatus(
            "Android",
            "Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) CapabilityLevel.READY else CapabilityLevel.UNAVAILABLE
        ),
        CapabilityStatus(
            "NFC tag tools",
            when {
                nfc == null -> "No NFC adapter"
                runCatching { nfc.isEnabled }.getOrDefault(false) -> "Hardware present · enabled"
                else -> "Hardware present · enable NFC in Android settings"
            },
            when {
                nfc == null -> CapabilityLevel.UNAVAILABLE
                runCatching { nfc.isEnabled }.getOrDefault(false) -> CapabilityLevel.READY
                else -> CapabilityLevel.ACTION_REQUIRED
            }
        ),
        CapabilityStatus(
            "NFC HCE / APDU Lab",
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) "Host Card Emulation supported" else "HCE not reported by this phone",
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) CapabilityLevel.READY else CapabilityLevel.UNAVAILABLE
        ),
        CapabilityStatus(
            "Bluetooth LE scanner",
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "BLE hardware not reported"
                !btScanGranted || !btConnectGranted -> "Grant Nearby devices permissions"
                !btEnabled -> "Bluetooth is off"
                else -> "BLE scan/connect ready"
            },
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> CapabilityLevel.UNAVAILABLE
                !btScanGranted || !btConnectGranted || !btEnabled -> CapabilityLevel.ACTION_REQUIRED
                else -> CapabilityLevel.READY
            }
        ),
        CapabilityStatus(
            "BLE advertiser",
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> "BLE hardware not reported"
                !btAdvertiseGranted || !btConnectGranted -> "Grant Bluetooth advertise/connect permissions"
                !btEnabled -> "Bluetooth is off"
                !multiAdvertise || !bleAdvertiserPresent -> "BLE advertising unsupported by adapter"
                else -> "V5 test beacon ready"
            },
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || (btConnectGranted && (!multiAdvertise || !bleAdvertiserPresent)) -> CapabilityLevel.UNAVAILABLE
                !btAdvertiseGranted || !btConnectGranted || !btEnabled -> CapabilityLevel.ACTION_REQUIRED
                else -> CapabilityLevel.READY
            }
        ),
        CapabilityStatus(
            "Wi‑Fi analyzer",
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_WIFI) -> "Wi‑Fi hardware not reported"
                !fineLocationGranted -> "Grant precise Location permission for scan results"
                !locationEnabled -> "Turn Android Location services on"
                else -> "Wi‑Fi scan prerequisites ready"
            },
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_WIFI) -> CapabilityLevel.UNAVAILABLE
                !fineLocationGranted || !locationEnabled -> CapabilityLevel.ACTION_REQUIRED
                else -> CapabilityLevel.READY
            }
        ),
        CapabilityStatus(
            "USB Host",
            if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
                "Supported · ${runCatching { usb?.deviceList?.size ?: 0 }.getOrDefault(0)} attached device(s)"
            } else "USB Host not reported",
            if (pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) CapabilityLevel.READY else CapabilityLevel.UNAVAILABLE
        ),
        CapabilityStatus(
            "Infrared emitter",
            if (runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)) {
                val ranges = runCatching { ir?.carrierFrequencies?.joinToString { "${it.minFrequency}-${it.maxFrequency}" }.orEmpty() }.getOrDefault("")
                "IR transmitter ready${if (ranges.isNotBlank()) " · $ranges Hz" else ""}"
            } else "IR transmitter unavailable",
            if (runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)) CapabilityLevel.READY else CapabilityLevel.UNAVAILABLE
        ),
        CapabilityStatus(
            "Network",
            when {
                active == null -> "No active network"
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true -> "${transports.ifEmpty { listOf("Other") }.joinToString()} · validated Internet"
                else -> "${transports.ifEmpty { listOf("Other") }.joinToString()} · active but not validated"
            },
            when {
                active == null -> CapabilityLevel.ACTION_REQUIRED
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true -> CapabilityLevel.READY
                else -> CapabilityLevel.ACTION_REQUIRED
            }
        )
    )
}
