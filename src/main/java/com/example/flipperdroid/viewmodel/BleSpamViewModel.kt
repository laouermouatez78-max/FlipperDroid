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
 * V4 BLE Red Team Lab controller.
 *
 * Real-radio testing is intentionally bounded: it uses a dedicated FlipperDroid
 * manufacturer payload, Android-managed advertising rates and an automatic timeout.
 * Vendor impersonation/crash payloads remain preview-only.
 */
class BleSpamViewModel(app: Application) : AndroidViewModel(app) {

    enum class BleSpamBrand { APPLE, SAMSUNG, ALL }
    enum class RealTxProfile { NORMAL, SHORT_STRESS }

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

    private val _realTxProfile = MutableStateFlow(RealTxProfile.NORMAL)
    val realTxProfile: StateFlow<RealTxProfile> = _realTxProfile

    private val _burstDurationSeconds = MutableStateFlow(10)
    val burstDurationSeconds: StateFlow<Int> = _burstDurationSeconds

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds

    private var allAdvertisementSets: List<AdvertisementSet> = emptyList()
    private var selectedSets: List<AdvertisementSet> = emptyList()
    private var simulationJob: Job? = null
    private var realTimeoutJob: Job? = null
    private var realElapsedJob: Job? = null

    private val realAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _realBeaconActive.value = true
            _realStatus.value = when (_realTxProfile.value) {
                RealTxProfile.NORMAL -> "Real BLE beacon is on-air"
                RealTxProfile.SHORT_STRESS -> "Bounded RF stress burst is on-air"
            }
            appendLog(
                "REAL TX started · ${_realTxProfile.value.name} · " +
                    "${_burstDurationSeconds.value}s max · FlipperDroid payload"
            )
            startElapsedCounter()
        }

        override fun onStartFailure(errorCode: Int) {
            cancelRealJobs()
            _realBeaconActive.value = false
            _realStatus.value = "BLE start failed · ${advertiseError(errorCode)}"
            appendLog("REAL TX failed · ${advertiseError(errorCode)}")
        }
    }

    init {
        loadAdvertisementSets()
        appendLog("V4 BLE Red Team Lab ready")
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
        _realPayloadText.value = value.filter { !it.isISOControl() }.take(12)
    }

    fun setRealTxProfile(profile: RealTxProfile) {
        if (_realBeaconActive.value) return
        _realTxProfile.value = profile
        _realStatus.value = when (profile) {
            RealTxProfile.NORMAL -> "Normal lab beacon selected"
            RealTxProfile.SHORT_STRESS -> "Short bounded stress profile selected"
        }
    }

    fun setBurstDurationSeconds(seconds: Int) {
        if (_realBeaconActive.value) return
        _burstDurationSeconds.value = seconds.coerceIn(5, 20)
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

        cancelRealJobs()
        _elapsedSeconds.value = 0

        val text = _realPayloadText.value.ifBlank { "HELLO-V4" }
        val payload = byteArrayOf(0x46, 0x44, 0x34) +
            text.toByteArray(Charsets.UTF_8).take(12).toByteArray()
        val durationMs = _burstDurationSeconds.value * 1000
        val mode = when (_realTxProfile.value) {
            RealTxProfile.NORMAL -> AdvertiseSettings.ADVERTISE_MODE_BALANCED
            RealTxProfile.SHORT_STRESS -> AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(mode)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(durationMs)
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
            _realStatus.value = "Starting bounded BLE transmission…"
            realTimeoutJob = viewModelScope.launch {
                delay(durationMs.toLong())
                if (_realBeaconActive.value) {
                    stopRealBeacon("Automatic stop after ${_burstDurationSeconds.value}s")
                }
            }
        }.onFailure {
            cancelRealJobs()
            _realStatus.value = "BLE start error: ${it.message ?: it.javaClass.simpleName}"
            appendLog("REAL TX exception · ${it.javaClass.simpleName}")
        }
    }

    fun stopRealBeacon() = stopRealBeacon("Stopped by user")

    private fun stopRealBeacon(reason: String) {
        if (permissionsGranted()) {
            runCatching { advertiser?.stopAdvertising(realAdvertiseCallback) }
        }
        val wasActive = _realBeaconActive.value
        cancelRealJobs()
        _realBeaconActive.value = false
        if (wasActive) appendLog("REAL TX stopped · $reason")
        _realStatus.value = "Real BLE test stopped · $reason"
    }

    private fun startElapsedCounter() {
        realElapsedJob?.cancel()
        realElapsedJob = viewModelScope.launch {
            while (_realBeaconActive.value) {
                delay(1000)
                if (_realBeaconActive.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    private fun cancelRealJobs() {
        realTimeoutJob?.cancel()
        realTimeoutJob = null
        realElapsedJob?.cancel()
        realElapsedJob = null
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
        stopRealBeacon("ViewModel cleared")
        super.onCleared()
    }
}
