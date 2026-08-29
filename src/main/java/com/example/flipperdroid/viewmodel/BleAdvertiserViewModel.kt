package com.example.flipperdroid.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BleAdvertiserViewModel(app: Application) : AndroidViewModel(app) {
    private val bluetoothManager = app.getSystemService(BluetoothManager::class.java)
    private val adapter get() = bluetoothManager?.adapter
    private val advertiser get() = adapter?.bluetoothLeAdvertiser

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    private val _status = MutableStateFlow("BLE test beacon ready")
    val status: StateFlow<String> = _status

    private val _localName = MutableStateFlow("FlipperDroid-V4")
    val localName: StateFlow<String> = _localName

    private val serviceUuid = UUID.fromString("f1d00001-7a91-4c4e-9b6a-4f6c69707034")
    private var previousAdapterName: String? = null
    private var renamedAdapter = false

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            _status.value = "Beacon active · V4 service UUID visible"
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            _status.value = "Beacon failed · ${advertiseError(errorCode)}"
            restoreAdapterName()
        }
    }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray()
    }

    fun permissionsGranted(): Boolean {
        val app = getApplication<Application>()
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun updateName(value: String) {
        if (_isAdvertising.value) return
        _localName.value = value.filter { !it.isISOControl() }.take(20).ifBlank { "FlipperDroid-V4" }
    }

    fun canAdvertise(): Boolean = runCatching {
        adapter?.isMultipleAdvertisementSupported == true && advertiser != null
    }.getOrDefault(false)

    fun start() {
        if (_isAdvertising.value) return
        val app = getApplication<Application>()
        if (!permissionsGranted()) {
            _status.value = "Bluetooth advertise/connect permission required"
            return
        }

        val bt = adapter
        if (bt == null) {
            _status.value = "Bluetooth LE is unavailable on this phone"
            return
        }
        if (!runCatching { bt.isEnabled }.getOrDefault(false)) {
            _status.value = "Turn Bluetooth on, then try again"
            return
        }
        val adv = advertiser
        if (adv == null || !canAdvertise()) {
            _status.value = "BLE advertising is not supported on this phone"
            return
        }

        runCatching {
            previousAdapterName = runCatching { bt.name }.getOrNull()
            if (_localName.value.isNotBlank()) {
                renamedAdapter = runCatching { bt.name = _localName.value }.getOrDefault(false)
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(serviceUuid))
                .addManufacturerData(0xFFFF, byteArrayOf(0x46, 0x44, 0x34))
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            adv.startAdvertising(settings, data, scanResponse, callback)
            _status.value = "Starting BLE test beacon…"
        }.onFailure {
            restoreAdapterName()
            _status.value = "Beacon error: ${it.message ?: it.javaClass.simpleName}"
        }
    }

    fun stop() {
        if (permissionsGranted()) {
            runCatching { advertiser?.stopAdvertising(callback) }
        }
        _isAdvertising.value = false
        restoreAdapterName()
        _status.value = "BLE test beacon stopped"
    }

    private fun restoreAdapterName() {
        if (!renamedAdapter || !permissionsGranted()) return
        val original = previousAdapterName
        if (!original.isNullOrBlank()) {
            runCatching { adapter?.name = original }
        }
        previousAdapterName = null
        renamedAdapter = false
    }

    private fun advertiseError(code: Int): String = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "advertisement data too large"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "too many advertisers are active"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "advertiser already started"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Bluetooth stack internal error"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "advertising feature unsupported"
        else -> "error code $code"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
