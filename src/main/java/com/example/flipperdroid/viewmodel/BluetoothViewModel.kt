package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Parsed manufacturer-specific field from a BLE advertisement. */
data class BleManufacturerPayload(val companyId: Int, val dataHex: String)

/** Parsed service-data field from a BLE advertisement. */
data class BleServicePayload(val uuid: String, val dataHex: String)

enum class BleTrafficLevel { NORMAL, ELEVATED }

data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val connectable: Boolean?,
    val serviceUuids: List<String>,
    val txPower: Int?,
    val manufacturerData: List<BleManufacturerPayload>,
    val serviceData: List<BleServicePayload>,
    val rawAdvertisementHex: String,
    val seenCount: Int,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val advertisementsPerSecond: Double,
    val trafficLevel: BleTrafficLevel
)

data class GattCharacteristicInfo(
    val uuid: String,
    val properties: Int,
    val readable: Boolean,
    val writable: Boolean,
    val notifiable: Boolean
)

data class GattServiceInfo(val uuid: String, val characteristics: List<GattCharacteristicInfo>)

/** Passive BLE advertisement explorer plus explicit GATT metadata discovery for owned/authorized devices. */
class BluetoothViewModel : ViewModel() {
    private var appContext: Context? = null
    private var adapter: BluetoothAdapter? = null
    private var scanning = false
    private var activeGatt: BluetoothGatt? = null
    private var scanTimeoutJob: Job? = null
    private var gattTimeoutJob: Job? = null

    private val _devices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val devices: StateFlow<List<BleDeviceInfo>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _status = MutableStateFlow("Bluetooth scanner not initialized")
    val status: StateFlow<String> = _status

    private val _connectedAddress = MutableStateFlow<String?>(null)
    val connectedAddress: StateFlow<String?> = _connectedAddress

    private val _gattServices = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val gattServices: StateFlow<List<GattServiceInfo>> = _gattServices

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
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
            !_permissionsGranted.value -> "Bluetooth permission required"
            !isBluetoothEnabled() -> "Bluetooth is off"
            else -> "BLE Explorer ready"
        }
    }

    fun refreshPermissionState() {
        val context = appContext ?: return
        _permissionsGranted.value = requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun isBluetoothEnabled(): Boolean = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshPermissionState()
        if (!_permissionsGranted.value) {
            _status.value = "Bluetooth permission required"
            return
        }
        val bluetoothAdapter = adapter ?: run {
            _status.value = "Bluetooth LE unavailable"
            return
        }
        if (!isBluetoothEnabled()) {
            _status.value = "Turn Bluetooth on, then scan again"
            return
        }
        val scanner = runCatching { bluetoothAdapter.bluetoothLeScanner }.getOrNull() ?: run {
            _status.value = "BLE scanner unavailable"
            return
        }

        stopScan(updateStatus = false)
        _devices.value = emptyList()
        scanning = true
        _isScanning.value = true
        _status.value = "Scanning and decoding nearby BLE advertisements…"

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onFailure {
                scanning = false
                _isScanning.value = false
                _status.value = "BLE scan could not start: ${it.message?.take(120) ?: it.javaClass.simpleName}"
                return
            }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(12_000)
            if (scanning) {
                stopScan(updateStatus = false)
                val elevated = _devices.value.count { it.trafficLevel == BleTrafficLevel.ELEVATED }
                _status.value = buildString {
                    append("Scan complete · ${_devices.value.size} device(s) found")
                    if (elevated > 0) append(" · $elevated elevated-rate observation(s)")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() = stopScan(updateStatus = true)

    @SuppressLint("MissingPermission")
    private fun stopScan(updateStatus: Boolean) {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        if (!scanning) return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        scanning = false
        _isScanning.value = false
        if (updateStatus) _status.value = "Scan stopped · ${_devices.value.size} device(s)"
    }

    @SuppressLint("MissingPermission")
    fun connectGatt(address: String) {
        refreshPermissionState()
        if (!_permissionsGranted.value) {
            _status.value = "Bluetooth connect permission required"
            return
        }
        val context = appContext ?: run {
            _status.value = "BLE Explorer not initialized"
            return
        }
        val bt = adapter ?: run {
            _status.value = "Bluetooth adapter unavailable"
            return
        }
        if (!isBluetoothEnabled()) {
            _status.value = "Turn Bluetooth on before connecting"
            return
        }
        if (_isConnecting.value) {
            _status.value = "A GATT connection attempt is already running"
            return
        }

        stopScan(updateStatus = false)
        disconnectGatt(updateStatus = false)
        _status.value = "Connecting to $address…"
        _gattServices.value = emptyList()
        _isConnecting.value = true

        runCatching {
            val device = bt.getRemoteDevice(address)
            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                @Suppress("DEPRECATION")
                device.connectGatt(context, false, gattCallback)
            }
            scheduleGattTimeout(address)
        }.onFailure {
            _isConnecting.value = false
            _status.value = "GATT connection error: ${it.message?.take(120) ?: it.javaClass.simpleName}"
        }
    }

    fun disconnectGatt() = disconnectGatt(updateStatus = true)

    @SuppressLint("MissingPermission")
    private fun disconnectGatt(updateStatus: Boolean) {
        gattTimeoutJob?.cancel()
        gattTimeoutJob = null
        _isConnecting.value = false
        val gatt = activeGatt
        activeGatt = null
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        _connectedAddress.value = null
        _gattServices.value = emptyList()
        if (updateStatus && gatt != null) _status.value = "GATT disconnected"
    }

    private fun scheduleGattTimeout(address: String) {
        gattTimeoutJob?.cancel()
        gattTimeoutJob = viewModelScope.launch {
            delay(12_000)
            if (_isConnecting.value && _connectedAddress.value == null) {
                disconnectGatt(updateStatus = false)
                _status.value = "GATT connection timed out after 12 seconds · $address"
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord
            val scanName = record?.deviceName
            val deviceName = runCatching { result.device.name }.getOrNull()
            val now = System.currentTimeMillis()
            val previous = _devices.value.firstOrNull { it.address == result.device.address }
            val firstSeen = previous?.firstSeenMs ?: now
            val seenCount = (previous?.seenCount ?: 0) + 1
            val elapsedMs = (now - firstSeen).coerceAtLeast(1L)
            val rate = if (seenCount <= 1) 0.0 else (seenCount - 1) * 1000.0 / elapsedMs.toDouble()
            val trafficLevel = if (seenCount >= 12 && rate >= 8.0) BleTrafficLevel.ELEVATED else BleTrafficLevel.NORMAL

            val manufacturerData = record?.manufacturerSpecificData?.let { sparse ->
                buildList {
                    for (index in 0 until sparse.size()) {
                        add(BleManufacturerPayload(companyId = sparse.keyAt(index), dataHex = sparse.valueAt(index).toHex()))
                    }
                }
            }.orEmpty()

            val serviceData = record?.serviceData.orEmpty().map { (uuid, payload) ->
                BleServicePayload(uuid = uuid.uuid.toString(), dataHex = payload.toHex())
            }

            val info = BleDeviceInfo(
                name = scanName ?: deviceName ?: "Unnamed BLE device",
                address = result.device.address,
                rssi = result.rssi,
                connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.isConnectable else null,
                serviceUuids = record?.serviceUuids.orEmpty().map { it.uuid.toString() },
                txPower = record?.txPowerLevel?.takeUnless { it == Int.MIN_VALUE },
                manufacturerData = manufacturerData,
                serviceData = serviceData,
                rawAdvertisementHex = record?.bytes.toHex(),
                seenCount = seenCount,
                firstSeenMs = firstSeen,
                lastSeenMs = now,
                advertisementsPerSecond = rate,
                trafficLevel = trafficLevel
            )

            val updated = _devices.value.toMutableList()
            val index = updated.indexOfFirst { it.address == info.address }
            if (index >= 0) updated[index] = info else updated += info
            _devices.value = updated.sortedByDescending { it.rssi }

            val elevated = updated.count { it.trafficLevel == BleTrafficLevel.ELEVATED }
            _status.value = buildString {
                append("Scanning · ${updated.size} device(s)")
                if (elevated > 0) append(" · $elevated elevated-rate")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanTimeoutJob?.cancel()
            scanTimeoutJob = null
            scanning = false
            _isScanning.value = false
            _status.value = "BLE scan failed · ${scanError(errorCode)}"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gattTimeoutJob?.cancel()
                _isConnecting.value = false
                _status.value = "GATT error · status $status"
                _connectedAddress.value = null
                _gattServices.value = emptyList()
                runCatching { gatt.close() }
                if (activeGatt === gatt) activeGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gattTimeoutJob?.cancel()
                    gattTimeoutJob = null
                    _isConnecting.value = false
                    _connectedAddress.value = gatt.device.address
                    _status.value = "Connected · discovering GATT services…"
                    val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
                    if (!started) _status.value = "Connected, but service discovery could not start"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    gattTimeoutJob?.cancel()
                    gattTimeoutJob = null
                    _isConnecting.value = false
                    _status.value = "GATT disconnected"
                    _connectedAddress.value = null
                    _gattServices.value = emptyList()
                    runCatching { gatt.close() }
                    if (activeGatt === gatt) activeGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _status.value = "Service discovery failed · status $status"
                return
            }
            val list = gatt.services.map { service ->
                GattServiceInfo(
                    uuid = service.uuid.toString(),
                    characteristics = service.characteristics.map { characteristic ->
                        val p = characteristic.properties
                        GattCharacteristicInfo(
                            uuid = characteristic.uuid.toString(),
                            properties = p,
                            readable = p and BluetoothGattCharacteristic.PROPERTY_READ != 0,
                            writable = p and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0,
                            notifiable = p and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        )
                    }
                )
            }
            _gattServices.value = list
            _status.value = "GATT ready · ${list.size} service(s)"
        }
    }

    private fun scanError(code: Int): String = when (code) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "scan already started"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Bluetooth registration failed"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Bluetooth stack internal error"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan unsupported"
        else -> "code $code"
    }

    override fun onCleared() {
        scanTimeoutJob?.cancel()
        gattTimeoutJob?.cancel()
        stopScan(updateStatus = false)
        disconnectGatt(updateStatus = false)
        super.onCleared()
    }
}

private fun ByteArray?.toHex(): String = this
    ?.joinToString(separator = " ") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    .orEmpty()
