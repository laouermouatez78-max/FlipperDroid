package com.example.flipperdroid.services

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.flipperdroid.model.BeaconState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * A deliberately constrained BLE advertiser for lab verification.
 * It emits one fixed, non-connectable, low-power service advertisement and Android
 * automatically stops it after 30 seconds. It is not a flood/spam implementation.
 */
class BleLabBeacon(private val context: Context) {
    companion object {
        const val LAB_UUID = "6f0d5b8c-36f2-4f70-8b4f-4f4c49505045"
    }

    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val _state = MutableStateFlow(BeaconState())
    val state: StateFlow<BeaconState> = _state.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        _state.value = BeaconState(false, "Balise arrêtée automatiquement après 30 s")
    }

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _state.value = BeaconState(true, "Balise de labo active — arrêt automatique à 30 s")
        }

        override fun onStartFailure(errorCode: Int) {
            _state.value = BeaconState(false, "Échec de la balise BLE (code $errorCode)")
        }
    }

    fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            )
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) {
            _state.value = BeaconState(false, "Permissions Bluetooth publicité/connexion requises")
            return
        }
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) {
            _state.value = BeaconState(false, "Bluetooth indisponible ou désactivé")
            return
        }
        if (!adapter.isMultipleAdvertisementSupported) {
            _state.value = BeaconState(false, "Publicité BLE non prise en charge par ce téléphone")
            return
        }
        val advertiser = adapter.bluetoothLeAdvertiser ?: run {
            _state.value = BeaconState(false, "Émetteur BLE indisponible")
            return
        }
        val service = ParcelUuid(UUID.fromString(LAB_UUID))
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .setTimeout(30_000)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(service)
            .build()

        runCatching {
            handler.removeCallbacks(timeoutRunnable)
            advertiser.stopAdvertising(callback)
            advertiser.startAdvertising(settings, data, callback)
            handler.postDelayed(timeoutRunnable, 30_500)
            _state.value = BeaconState(false, "Démarrage de la balise…")
        }.onFailure {
            _state.value = BeaconState(false, "Impossible d'émettre: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!hasPermission()) return
        handler.removeCallbacks(timeoutRunnable)
        runCatching { manager.adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback) }
        _state.value = BeaconState(false, "Balise arrêtée")
    }
}
