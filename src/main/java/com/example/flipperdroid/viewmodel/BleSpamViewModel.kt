package com.example.flipperdroid.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flipperdroid.model.`class`.AdvertisementSet
import com.example.flipperdroid.model.`object`.ContinuityNewDevicePopUpAdvertisementSetGenerator
import com.example.flipperdroid.model.`object`.EasySetupBudsAdvertisementSetGenerator
import com.example.flipperdroid.model.`object`.EasySetupWatchAdvertisementSetGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * V4 BLE lab controller.
 *
 * Legacy vendor payloads stay preview-only. The real-radio path uses a dedicated
 * FlipperDroid test manufacturer payload at a normal Android advertising rate so a
 * second device owned by the user can verify real BLE transmission without flooding
 * nearby devices or impersonating a vendor accessory.
 */
class BleSpamViewModel(app: Application) : AndroidViewModel(app) {

    enum class BleSpamBrand { APPLE, SAMSUNG, ALL }

    private val bluetoothManager = app.getSystemService(BluetoothManager::class.java)
    private val adapter get() = bluetoothManager?.adapter
    private val advertiser get() = adapter?.bluetoothLeAdvertiser

    private val _advertisementSets = MutableStateFlow<List<AdvertisementSet>>(emptyList())
    val advertisementSets: StateFlow<List<AdvertisementSet>> = _advertisementSets

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _brand = MutableStateFlow(BleSpamBrand.APPLE)
    val brand: StateFlow<BleSpamBrand> = _brand

    private val _spamLogs = MutableStateFlow<List<String>>(emptyList())
    val spamLogs: StateFlow<List<String>> = _spamLogs

    private val _realBeaconActive = MutableStateFlow(false)
    val realBeaconActive: StateFlow<Boolean> = _realBeaconActive

    private val _realPayloadText = MutableStateFlow("HELLO-V4")
    val realPayloadText: StateFlow<String> = _realPayloadText

    private val _realStatus = MutableStateFlow("Real BLE test beacon ready")
    val realStatus: StateFlow<String> = _realStatus

    private var allAdvertisementSets: List<AdvertisementSet> = emptyList()
    private var selectedSets: List<AdvertisementSet> = emptyList()
    private var simulationJob: Job? = null

    private val realAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _realBeaconActive.value = true
            _realStatus.value = "Real BLE beacon is on-air"
            appendLog("REAL TX started · FlipperDroid test payload")
        }

        override fun onStartFailure(errorCode: Int) {
            _realBeaconActive.value = false
            _realStatus.value = "BLE start failed · ${advertiseError(errorCode)}"
            appendLog("REAL TX failed · ${advertiseError(errorCode)}")
        }
    }

    init {
        loadAdvertisementSets()
        appendLog("V4 BLE Lab ready")
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

    fun canAdvertise(): Boolean = runCatching {
        adapter?.isMultipleAdvertisementSupported == true && advertiser != null
    }.getOrDefault(false)

    fun updateRealPayload(value: String) {
        if (_realBeaconActive.value) return
        val cleaned = value.filter { !it.isISOControl() }.take(12)
        _realPayloadText.value = cleaned
    }

    fun startRealBeacon() {
        if (_realBeaconActive.value) return
        if (!permissionsGranted()) {
            _realStatus.value = "Grant Bluetooth advertise/connect permission"
            return
        }
        val bt = adapter
        if (bt == null) {
            _realStatus.value = "Bluetooth unavailable"
            return
        }
        if (!runCatching { bt.isEnabled }.getOrDefault(false)) {
            _realStatus.value = "Turn Bluetooth on first"
            return
        }
        val adv = advertiser
        if (adv == null || !canAdvertise()) {
            _realStatus.value = "BLE advertising not supported by this phone"
            return
        }
        val text = _realPayloadText.value.ifBlank { "HELLO-V4" }
        val payload = byteArrayOf(0x46, 0x44, 0x34) + text.toByteArray(Charsets.UTF_8).take(12).toByteArray()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(0xFFFF, payload)
            .build()
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        runCatching {
            adv.startAdvertising(settings, data, scanResponse, realAdvertiseCallback)
            _realStatus.value = "Starting real BLE beacon…"
        }.onFailure {
            _realStatus.value = "BLE start error: ${it.message ?: it.javaClass.simpleName}"
            appendLog("REAL TX exception · ${it.javaClass.simpleName}")
        }
    }

    fun stopRealBeacon() {
        if (permissionsGranted()) {
            runCatching { advertiser?.stopAdvertising(realAdvertiseCallback) }
        }
        if (_realBeaconActive.value) appendLog("REAL TX stopped")
        _realBeaconActive.value = false
        _realStatus.value = "Real BLE test beacon stopped"
    }

    fun setBrand(brand: BleSpamBrand) {
        if (_isActive.value) return
        _brand.value = brand
        loadAdvertisementSets()
        appendLog("Preview profile changed to ${brand.name}")
    }

    fun startSimulation() {
        if (_isActive.value) return
        val queue = selectedSets.ifEmpty { _advertisementSets.value }
        if (queue.isEmpty()) {
            appendLog("No BLE preview payload selected")
            return
        }

        _isActive.value = true
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            appendLog("Legacy profile preview started with ${queue.size} payload(s)")
            var index = 0
            while (_isActive.value) {
                val set = queue[index % queue.size]
                val title = if (set.title.isNotBlank()) set.title else set.type.toString()
                appendLog("PREVIEW #${index + 1}: $title (${set.type})")
                index++
                delay(650)
            }
        }
    }

    fun stopSimulation() {
        if (!_isActive.value && simulationJob == null) return
        _isActive.value = false
        simulationJob?.cancel()
        simulationJob = null
        appendLog("Legacy profile preview stopped")
    }

    private fun loadAdvertisementSets() {
        val sets = when (_brand.value) {
            BleSpamBrand.APPLE -> ContinuityNewDevicePopUpAdvertisementSetGenerator.getAdvertisementSets()
            BleSpamBrand.SAMSUNG ->
                EasySetupWatchAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupBudsAdvertisementSetGenerator.getAdvertisementSets()
            BleSpamBrand.ALL ->
                ContinuityNewDevicePopUpAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupWatchAdvertisementSetGenerator.getAdvertisementSets() +
                    EasySetupBudsAdvertisementSetGenerator.getAdvertisementSets()
        }
        allAdvertisementSets = sets
        selectedSets = sets
        _advertisementSets.value = sets
    }

    fun setCheckedPayloads(checkedStates: List<Boolean>) {
        if (_isActive.value) return
        selectedSets = allAdvertisementSets.filterIndexed { index, _ ->
            checkedStates.getOrNull(index) == true
        }
        _advertisementSets.value = allAdvertisementSets
    }

    private fun advertiseError(code: Int): String = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "advertisement data too large"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "too many advertisers active"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "already started"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Bluetooth stack internal error"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "feature unsupported"
        else -> "error code $code"
    }

    private fun appendLog(message: String) {
        val stamp = System.currentTimeMillis() % 100000
        _spamLogs.value = (_spamLogs.value + "[$stamp] $message").takeLast(120)
    }

    override fun onCleared() {
        stopSimulation()
        stopRealBeacon()
        super.onCleared()
    }
}
