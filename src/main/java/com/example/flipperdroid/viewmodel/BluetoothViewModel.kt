package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val connectable: Boolean?,
    val serviceUuids: List<String>
)

/** V3 BLE discovery engine for passive nearby-device inventory. */
class BluetoothViewModel : ViewModel() {

    private var appContext: Context? = null
    private var adapter: BluetoothAdapter? = null
    private var scanning = false

    private val _devices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BleDeviceInfo>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _status = MutableStateFlow("Bluetooth scanner not initialized")
    val status: StateFlow<String> = _status

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> emptyArray()
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val manager = appContext?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        adapter = manager?.adapter
        refreshPermissionState()
        _status.value = when {
            adapter == null -> "Bluetooth LE unavailable"
            adapter?.isEnabled != true -> "Bluetooth is off"
            else -> "BLE scanner ready"
        }
    }

    fun refreshPermissionState() {
        val context = appContext ?: return
        _permissionsGranted.value = requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshPermissionState()
        if (!_permissionsGranted.value) {
            _status.value = "Bluetooth permission required"
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            _status.value = "Bluetooth LE unavailable"
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            _status.value = "Turn Bluetooth on, then scan again"
            return
        }
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _status.value = "BLE scanner unavailable"
            return
        }

        stopScan()
        _devices.value = emptyList()
        scanning = true
        _isScanning.value = true
        _status.value = "Scanning nearby BLE advertisements…"

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, callback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!scanning) return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        scanning = false
        _isScanning.value = false
        _status.value = "Scan stopped · ${_devices.value.size} device(s)"
    }

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanName = result.scanRecord?.deviceName
            val deviceName = runCatching { result.device.name }.getOrNull()
            val info = BleDeviceInfo(
                name = scanName ?: deviceName ?: "Unnamed BLE device",
                address = result.device.address,
                rssi = result.rssi,
                connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.isConnectable else null,
                serviceUuids = result.scanRecord?.serviceUuids.orEmpty().map { it.uuid.toString() }
            )
            val updated = _devices.value.toMutableList()
            val index = updated.indexOfFirst { it.address == info.address }
            if (index >= 0) updated[index] = info else updated += info
            _devices.value = updated.sortedByDescending { it.rssi }
            _status.value = "Scanning · ${updated.size} device(s)"
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            _isScanning.value = false
            _status.value = "BLE scan failed (code $errorCode)"
        }
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}
