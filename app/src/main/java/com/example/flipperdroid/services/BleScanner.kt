package com.example.flipperdroid.services

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.flipperdroid.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class BleScanner(private val context: Context) {
    companion object {
        private const val RSSI_HISTORY_SIZE = 20
    }

    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val seen = ConcurrentHashMap<String, BleDevice>()
    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    @Volatile
    var isScanning: Boolean = false
        private set

    var onEvent: ((String) -> Unit)? = null

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            update(result)
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::update)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            val reason = when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "scan déjà démarré"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "échec d'enregistrement de l'application"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "erreur interne du contrôleur BLE"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "fonctionnalité non supportée par ce matériel"
                5 -> "trop d'applications ont enregistré un scan (out of hardware resources)"
                else -> "code $errorCode"
            }
            onEvent?.invoke("Échec du scan BLE ($reason)")
        }
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isScanning) return
        if (!hasPermissions()) {
            onEvent?.invoke("Permissions Bluetooth requises")
            return
        }
        val adapter = manager.adapter
        if (adapter == null) {
            onEvent?.invoke("Bluetooth non pris en charge")
            return
        }
        if (!adapter.isEnabled) {
            onEvent?.invoke("Bluetooth désactivé")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            onEvent?.invoke("Scanner BLE indisponible")
            return
        }
        seen.clear()
        _devices.value = emptyList()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0L)
            .build()
        runCatching {
            scanner.startScan(null, settings, callback)
            isScanning = true
            onEvent?.invoke("Scan BLE démarré")
        }.onFailure {
            onEvent?.invoke("Impossible de démarrer le scan: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!isScanning) return
        runCatching {
            if (hasPermissions()) manager.adapter?.bluetoothLeScanner?.stopScan(callback)
        }
        isScanning = false
        onEvent?.invoke("Scan BLE arrêté")
    }

    fun clear() {
        seen.clear()
        _devices.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun update(result: ScanResult) {
        val record = result.scanRecord
        val address = runCatching { result.device.address }.getOrDefault("inconnu")
        val name = runCatching { result.device.name }.getOrNull()
            ?: record?.deviceName
            ?: "Sans nom"

        val manufacturerId = record?.manufacturerSpecificData?.let { sparse ->
            if (sparse.size() > 0) sparse.keyAt(0) else null
        }
        val serviceUuids = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
        val connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.isConnectable else null
        val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result.txPower.takeIf { it != Int.MIN_VALUE && it != 127 }
        } else {
            record?.txPowerLevel?.takeIf { it != Int.MIN_VALUE && it != 127 }
        }

        val previous = seen[address]
        val history = ((previous?.rssiHistory ?: emptyList()) + result.rssi).takeLast(RSSI_HISTORY_SIZE)

        seen[address] = BleDevice(
            address = address,
            name = name,
            rssi = result.rssi,
            txPower = txPower,
            manufacturerId = manufacturerId,
            manufacturerName = BleVendorLookup.name(manufacturerId),
            serviceUuids = serviceUuids,
            connectable = connectable,
            randomAddress = BleVendorLookup.isLikelyRandomAddress(address),
            rssiHistory = history,
            firstSeen = previous?.firstSeen ?: System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            packetCount = (previous?.packetCount ?: 0) + 1
        )
        _devices.value = seen.values.sortedWith(compareByDescending<BleDevice> { it.rssi }.thenBy { it.name.lowercase() })
    }

    /** Builds a CSV export of the current scan snapshot for local sharing/analysis. */
    fun exportCsv(): String {
        val header = "address,name,rssi_dbm,tx_power_dbm,manufacturer,random_address,connectable,packets,service_uuids"
        val rows = _devices.value.joinToString("\n") { d ->
            listOf(
                d.address,
                d.name.replace(",", " "),
                d.rssi.toString(),
                d.txPower?.toString() ?: "",
                (d.manufacturerName ?: "").replace(",", " "),
                if (d.randomAddress) "oui" else "non",
                d.connectable?.let { if (it) "oui" else "non" } ?: "",
                d.packetCount.toString(),
                d.serviceUuids.joinToString(";")
            ).joinToString(",")
        }
        return "$header\n$rows"
    }
}
