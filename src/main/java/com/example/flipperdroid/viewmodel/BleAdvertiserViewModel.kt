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

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _localName = MutableStateFlow("FlipperDroid-V4")
    val localName: StateFlow<String> = _localName

    private val serviceUuid = UUID.fromString("f1d00001-7a91-4c4e-9b6a-4f6c69707034")

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            _status.value = "Advertising active · service $serviceUuid"
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            _status.value = "Advertising failed · code $errorCode"
        }
    }

    fun updateName(value: String) {
        _localName.value = value.take(20)
    }

    fun canAdvertise(): Boolean = adapter?.isMultipleAdvertisementSupported == true && advertiser != null

    fun start() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED
        ) {
            _status.value = "Bluetooth advertise permission required"
            return
        }
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            _status.value = "Bluetooth is disabled"
            return
        }
        val adv = advertiser
        if (adv == null) {
            _status.value = "BLE advertising is not supported on this phone"
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            ) {
                bt.name = _localName.value
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
            _status.value = "Starting BLE advertisement…"
        }.onFailure {
            _status.value = "Advertising error: ${it.message}"
        }
    }

    fun stop() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching { advertiser?.stopAdvertising(callback) }
        _isAdvertising.value = false
        _status.value = "Advertising stopped"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
