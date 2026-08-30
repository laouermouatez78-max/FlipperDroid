package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
    val lastSeen: String,
    val channel: Int = frequencyToChannel(frequency)
) {
    val band: String get() = frequencyBand(frequency)

    companion object {
        @Suppress("DEPRECATION")
        fun fromScanResult(result: ScanResult): WifiNetwork {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return WifiNetwork(
                ssid = result.SSID.takeUnless { it.isNullOrBlank() } ?: "<Hidden Network>",
                bssid = result.BSSID ?: "<unknown>",
                rssi = result.level,
                frequency = result.frequency,
                capabilities = result.capabilities.orEmpty(),
                lastSeen = formatter.format(Date())
            )
        }

        fun frequencyToChannel(frequency: Int): Int = when {
            frequency == 2484 -> 14
            frequency in 2412..2472 -> (frequency - 2407) / 5
            frequency in 5170..5895 -> (frequency - 5000) / 5
            frequency in 5955..7115 -> (frequency - 5950) / 5
            else -> 0
        }

        fun frequencyBand(frequency: Int): String = when (frequency) {
            in 2400..2500 -> "2.4 GHz"
            in 4900..5924 -> "5 GHz"
            in 5925..7125 -> "6 GHz"
            in 57_000..71_000 -> "60 GHz"
            else -> "Other"
        }
    }
}

class WifiDeautherViewModel : ViewModel() {
    private var appContext: Context? = null
    private var wifiManager: WifiManager? = null
    private var locationManager: LocationManager? = null
    private var scanReceiver: BroadcastReceiver? = null

    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiNetwork>> = _networks

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _locationEnabled = MutableStateFlow(false)
    val locationEnabled: StateFlow<Boolean> = _locationEnabled

    private val _wifiReady = MutableStateFlow(false)
    val wifiReady: StateFlow<Boolean> = _wifiReady

    private val _status = MutableStateFlow("Wi‑Fi analyzer not initialized")
    val status: StateFlow<String> = _status

    fun requiredPermissions(): Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun initialize(context: Context) {
        appContext = context.applicationContext
        wifiManager = appContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        locationManager = appContext?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        registerScanReceiver()
        refreshPrerequisiteState()

        if (_permissionsGranted.value && _locationEnabled.value && _wifiReady.value) {
            readScanResults("Cached scan results")
        }
    }

    fun refreshPermissionState() = refreshPrerequisiteState()

    fun refreshPrerequisiteState() {
        val context = appContext ?: return
        _permissionsGranted.value = requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        _locationEnabled.value = isLocationServiceEnabled()

        val wifi = wifiManager
        _wifiReady.value = when {
            wifi == null -> false
            wifi.isWifiEnabled -> true
            else -> runCatching {
                @Suppress("DEPRECATION")
                wifi.isScanAlwaysAvailable
            }.getOrDefault(false)
        }

        if (!_isScanning.value) {
            _status.value = when {
                wifi == null -> "Wi‑Fi service unavailable on this device"
                !_permissionsGranted.value -> "Grant precise location for Wi‑Fi scan results"
                !_locationEnabled.value -> "Turn Android Location on for Wi‑Fi scanning"
                !_wifiReady.value -> "Turn Wi‑Fi on (or enable Wi‑Fi scanning in Location services)"
                else -> "Wi‑Fi analyzer ready"
            }
        }
    }

    private fun isLocationServiceEnabled(): Boolean {
        val manager = locationManager ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(false)
    }

    private fun registerScanReceiver() {
        val context = appContext ?: return
        if (scanReceiver != null) return
        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    refreshPrerequisiteState()
                    val fresh = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    readScanResults(if (fresh) "Fresh scan complete" else "Cached scan results")
                }
            }
        }
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(scanReceiver, filter)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshPrerequisiteState()
        if (!_permissionsGranted.value) {
            _status.value = "Precise location permission is required for Wi‑Fi scan results"
            return
        }
        if (!_locationEnabled.value) {
            _status.value = "Android Location is off — enable it, then scan again"
            return
        }
        val wifi = wifiManager ?: run {
            _status.value = "Wi‑Fi service unavailable"
            return
        }
        if (!_wifiReady.value) {
            _status.value = "Wi‑Fi scanning is unavailable while Wi‑Fi is off"
            return
        }

        _isScanning.value = true
        _status.value = "Scanning nearby Wi‑Fi…"
        try {
            @Suppress("DEPRECATION")
            val started = wifi.startScan()
            if (!started) readScanResults("Fresh scan throttled by Android — showing cached results")
        } catch (_: SecurityException) {
            _permissionsGranted.value = false
            _isScanning.value = false
            _status.value = "Wi‑Fi scan blocked by Android permissions"
        } catch (error: Exception) {
            _isScanning.value = false
            _status.value = "Wi‑Fi scan error: ${error.message?.take(120) ?: error.javaClass.simpleName}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun readScanResults(message: String) {
        refreshPrerequisiteState()
        if (!_permissionsGranted.value || !_locationEnabled.value) {
            _isScanning.value = false
            return
        }

        try {
            val results = wifiManager?.scanResults.orEmpty()
                .map(WifiNetwork::fromScanResult)
                .filter { it.bssid != "<unknown>" }
                .distinctBy { it.bssid }
                .sortedWith(compareByDescending<WifiNetwork> { it.rssi }.thenBy { it.ssid })
            _networks.value = results
            _status.value = if (results.isEmpty()) "$message · no access points returned" else "$message · ${results.size} network(s)"
        } catch (_: SecurityException) {
            _permissionsGranted.value = false
            _status.value = "Unable to read Wi‑Fi results: precise location permission denied"
        } catch (error: Exception) {
            _status.value = "Unable to read Wi‑Fi results: ${error.message?.take(120) ?: error.javaClass.simpleName}"
        } finally {
            _isScanning.value = false
        }
    }

    fun getNetworkStrength(rssi: Int): NetworkStrength = when {
        rssi >= -50 -> NetworkStrength.EXCELLENT
        rssi >= -60 -> NetworkStrength.GOOD
        rssi >= -70 -> NetworkStrength.FAIR
        else -> NetworkStrength.POOR
    }

    fun getSecurityType(capabilities: String): SecurityType {
        val caps = capabilities.uppercase(Locale.ROOT)
        val hasSae = "SAE" in caps || "WPA3" in caps
        val hasPsk = "PSK" in caps
        return when {
            "OWE" in caps -> SecurityType.OWE
            hasSae && hasPsk -> SecurityType.WPA2_WPA3
            hasSae -> SecurityType.WPA3
            "WPA2" in caps || "RSN" in caps -> SecurityType.WPA2
            "WPA" in caps -> SecurityType.WPA
            "WEP" in caps -> SecurityType.WEP
            else -> SecurityType.OPEN
        }
    }

    fun securityFinding(network: WifiNetwork): String = when (getSecurityType(network.capabilities)) {
        SecurityType.OPEN -> "Open network — traffic confidentiality relies entirely on app-layer encryption"
        SecurityType.OWE -> "Enhanced Open / OWE detected — encrypted without a shared password"
        SecurityType.WEP -> "Legacy WEP detected — replace with WPA2/WPA3"
        SecurityType.WPA -> "Legacy WPA detected — migrate to WPA2/WPA3"
        SecurityType.WPA2 -> "WPA2 detected"
        SecurityType.WPA2_WPA3 -> "WPA2/WPA3 transition mode detected"
        SecurityType.WPA3 -> "WPA3 detected"
    }

    override fun onCleared() {
        val context = appContext
        val receiver = scanReceiver
        if (context != null && receiver != null) runCatching { context.unregisterReceiver(receiver) }
        scanReceiver = null
        super.onCleared()
    }
}

enum class NetworkStrength { EXCELLENT, GOOD, FAIR, POOR }
enum class SecurityType { OPEN, OWE, WEP, WPA, WPA2, WPA2_WPA3, WPA3 }
