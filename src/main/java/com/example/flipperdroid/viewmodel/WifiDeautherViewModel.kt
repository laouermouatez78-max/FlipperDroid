package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
    companion object {
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
    }
}

/**
 * V3 Wi-Fi audit controller.
 *
 * The historical "deauther" name is kept for source compatibility, but V3 performs
 * defensive discovery only: SSID/BSSID inventory, RSSI, channel and security posture.
 */
class WifiDeautherViewModel : ViewModel() {

    private var appContext: Context? = null
    private var wifiManager: WifiManager? = null
    private var scanReceiver: BroadcastReceiver? = null

    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiNetwork>> = _networks

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted

    private val _status = MutableStateFlow("Wi-Fi audit not initialized")
    val status: StateFlow<String> = _status

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        wifiManager = appContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        registerScanReceiver()
        refreshPermissionState()
        _status.value = if (wifiManager == null) "Wi-Fi service unavailable" else "Wi-Fi audit ready"
    }

    fun refreshPermissionState() {
        val context = appContext ?: return
        _permissionsGranted.value = requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun registerScanReceiver() {
        val context = appContext ?: return
        if (scanReceiver != null) return

        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    readScanResults(
                        if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                            "Fresh scan complete"
                        } else {
                            "Using cached scan results"
                        }
                    )
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
        refreshPermissionState()
        if (!_permissionsGranted.value) {
            _status.value = "Wi-Fi permission required"
            return
        }

        val wifi = wifiManager
        if (wifi == null) {
            _status.value = "Wi-Fi service unavailable"
            return
        }
        if (!wifi.isWifiEnabled) {
            _status.value = "Turn Wi-Fi on, then scan again"
            return
        }

        _isScanning.value = true
        _status.value = "Scanning nearby Wi-Fi…"
        try {
            @Suppress("DEPRECATION")
            val started = wifi.startScan()
            if (!started) {
                readScanResults("Android scan throttling active — showing cached results")
            }
        } catch (security: SecurityException) {
            _permissionsGranted.value = false
            _isScanning.value = false
            _status.value = "Wi-Fi permission denied"
        }
    }

    @SuppressLint("MissingPermission")
    private fun readScanResults(message: String) {
        try {
            val results = wifiManager?.scanResults.orEmpty()
                .map(WifiNetwork::fromScanResult)
                .filter { it.bssid != "<unknown>" }
                .distinctBy { it.bssid }
                .sortedWith(compareByDescending<WifiNetwork> { it.rssi }.thenBy { it.ssid })
            _networks.value = results
            _status.value = "$message · ${results.size} network(s)"
        } catch (security: SecurityException) {
            _permissionsGranted.value = false
            _status.value = "Unable to read Wi-Fi results: permission denied"
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
        return when {
            "WPA3" in caps || "SAE" in caps -> SecurityType.WPA3
            "WPA2" in caps || "RSN" in caps -> SecurityType.WPA2
            "WPA" in caps -> SecurityType.WPA
            "WEP" in caps -> SecurityType.WEP
            else -> SecurityType.OPEN
        }
    }

    fun securityFinding(network: WifiNetwork): String = when (getSecurityType(network.capabilities)) {
        SecurityType.OPEN -> "Open network — traffic confidentiality depends on higher-layer encryption"
        SecurityType.WEP -> "Legacy WEP detected — replace with WPA2/WPA3"
        SecurityType.WPA -> "Legacy WPA detected — migrate to WPA2/WPA3"
        SecurityType.WPA2 -> "WPA2 detected"
        SecurityType.WPA3 -> "WPA3 detected"
    }

    override fun onCleared() {
        val context = appContext
        val receiver = scanReceiver
        if (context != null && receiver != null) {
            runCatching { context.unregisterReceiver(receiver) }
        }
        scanReceiver = null
        super.onCleared()
    }
}

enum class NetworkStrength { EXCELLENT, GOOD, FAIR, POOR }
enum class SecurityType { OPEN, WEP, WPA, WPA2, WPA3 }
