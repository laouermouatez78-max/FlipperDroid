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
 * Real-radio tests are intentionally bounded to one identifiable FlipperDroid advertiser.
 * Vendor impersonation, crash payloads and continuous flooding remain preview-only.
 */
class BleSpamViewModel(app: Application) : AndroidViewModel(app) {

    enum class BleSpamBrand { APPLE, SAMSUNG, ALL }

    enum class RealTestProfile(
        val label: String,
        val durationSeconds: Int,
        val advertiseMode: Int,
        val txPower: Int
    ) {
        BALANCED_30S(
            label = "Balanced · 30 s",
            durationSeconds = 30,
            advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED,
            txPower = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
        ),
        SHORT_BURST_10S(
            label = "Short burst · 10 s",
            durationSeconds = 10,
            advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY,
            txPower = AdvertiseSettings.ADVERTISE_TX_POWER_LOW
        )
    }

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

    private val _realStatus = MutableStateFlow("Real BLE resilience test ready")
    val realStatus: StateFlow<String> = _realStatus

    private val _realProfile = MutableStateFlow(RealTestProfile.BALANCED_30S)
    val realProfile: StateFlow<RealTestProfile> = _realProfile

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _realSessionCount = MutableStateFlow(0)
    val realSessionCount: StateFlow<Int> = _realSessionCount

    private val _lastSessionSummary = MutableStateFlow("No real-radio session yet")
    val lastSessionSummary: StateFlow<String> = _lastSessionSummary

    private var allAdvertisementSets: List<AdvertisementSet> = emptyList()
    private var selectedSets: List<AdvertisementSet> = emptyList()
    private var simulationJob: Job? = null
    private var realSessionJob: Job? = null
    private var realSessionStartMs: Long = 0L

    private val realAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            val profile = _realProfile.value
            _realBeaconActive.value = true
            _realSessionCount.value += 1
            realSessionStartMs = System.currentTimeMillis()
            _remainingSeconds.value = profile.durationSeconds
            _realStatus.value = "On-air · ${profile.label} · auto-stop enabled"
            appendLog("REAL TX started · ${profile.label} · FlipperDroid LAB payload")
            startAutoStopTimer(profile.durationSeconds)
        }

        override fun onStartFailure(errorCode: Int) {
            realSessionJob?.cancel()
            realSessionJob = null
            realSessionStartMs = 0L
            _remainingSeconds.value = 0
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
        val cleaned = value.filter { !it.isISOControl() }.take(8)
        _realPayloadText.value = cleaned
    }

    fun setRealProfile(profile: RealTestProfile) {
        if (_realBeaconActive.value) return
        _realProfile.value = profile
        _realStatus.value = "Ready · ${profile.label}"
        appendLog("Real-radio profile set to ${profile.label}")
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

        val profile = _realProfile.value
        val text = _realPayloadText.value.ifBlank { "HELLO-V4" }
        // Fixed FD4LAB prefix + reserved test manufacturer ID makes the packet identifiable
        // and prevents this path from becoming a vendor-profile impersonator.
        val payload = byteArrayOf(0x46, 0x44, 0x34, 0x4C, 0x41, 0x42) +
            text.toByteArray(Charsets.UTF_8).take(8).toByteArray()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(profile.advertiseMode)
            .setTxPowerLevel(profile.txPower)
            .setConnectable(false)
            .setTimeout(profile.durationSeconds * 1000)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(0xFFFF, payload)
            .build()
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        runCatching {
            realSessionJob?.cancel()
            _remainingSeconds.value = profile.durationSeconds
            adv.startAdvertising(settings, data, scanResponse, realAdvertiseCallback)
            _realStatus.value = "Starting · ${profile.label}…"
        }.onFailure {
            _remainingSeconds.value = 0
            _realStatus.value = "BLE start error: ${it.message ?: it.javaClass.simpleName}"
            appendLog("REAL TX exception · ${it.javaClass.simpleName}")
        }
    }

    fun stopRealBeacon() {
        stopRealBeaconInternal("manual stop")
    }

    private fun startAutoStopTimer(durationSeconds: Int) {
        realSessionJob?.cancel()
        realSessionJob = viewModelScope.launch {
            for (remaining in durationSeconds downTo 1) {
                _remainingSeconds.value = remaining
                delay(1000)
            }
            _remainingSeconds.value = 0
            stopRealBeaconInternal("automatic timeout")
        }
    }

    private fun stopRealBeaconInternal(reason: String) {
        realSessionJob?.cancel()
        realSessionJob = null
        if (permissionsGranted()) {
            runCatching { advertiser?.stopAdvertising(realAdvertiseCallback) }
        }

        val wasActive = _realBeaconActive.value || realSessionStartMs > 0L
        val elapsedMs = if (realSessionStartMs > 0L) {
            (System.currentTimeMillis() - realSessionStartMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val elapsedSeconds = elapsedMs / 1000.0

        _remainingSeconds.value = 0
        _realBeaconActive.value = false
        if (wasActive) {
            val summary = "${_realProfile.value.label} · %.1f s · $reason".format(elapsedSeconds)
            _lastSessionSummary.value = summary
            appendLog("REAL TX stopped · $summary")
        }
        realSessionStartMs = 0L
        _realStatus.value = "Real BLE resilience test stopped"
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
        stopRealBeaconInternal("screen closed")
        super.onCleared()
    }
}
