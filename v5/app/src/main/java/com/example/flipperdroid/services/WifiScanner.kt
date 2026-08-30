package com.example.flipperdroid.services

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
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.flipperdroid.model.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiScanner(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiNetwork>> = _networks.asStateFlow()
    private val _status = MutableStateFlow("Prêt")
    val status: StateFlow<String> = _status.asStateFlow()

    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
            val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            loadResults()
            _status.value = if (updated) {
                "Scan terminé — ${_networks.value.size} réseau(x)"
            } else {
                "Résultats en cache — Android a pu limiter le scan"
            }
        }
    }

    init {
        registerReceiver()
        if (hasPermission()) loadResults()
    }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    @SuppressLint("MissingPermission")
    fun scan() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            _status.value = "Wi‑Fi non pris en charge"
            return
        }
        if (!hasPermission()) {
            _status.value = "Permission de localisation requise pour les résultats Wi‑Fi"
            return
        }
        if (!wifiManager.isWifiEnabled) {
            _status.value = "Wi‑Fi désactivé"
            return
        }
        if (!isLocationEnabled()) {
            _status.value = "Activez la localisation Android pour autoriser le scan Wi‑Fi"
            return
        }
        loadResults()
        val started = runCatching { wifiManager.startScan() }.getOrDefault(false)
        _status.value = if (started) {
            "Scan demandé…"
        } else {
            "Android a limité le scan — affichage des résultats disponibles"
        }
    }

    @SuppressLint("MissingPermission")
    fun loadResults() {
        if (!hasPermission()) return
        if (!isLocationEnabled()) {
            _status.value = "Localisation Android désactivée — résultats Wi‑Fi indisponibles"
            return
        }
        _networks.value = runCatching {
            wifiManager.scanResults
                .map(::toModel)
                .distinctBy { it.bssid }
                .sortedByDescending { it.rssi }
        }.getOrElse {
            _status.value = "Lecture Wi‑Fi impossible: ${it.message ?: it.javaClass.simpleName}"
            emptyList()
        }
    }

    fun close() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    private fun toModel(result: ScanResult): WifiNetwork {
        val caps = result.capabilities.orEmpty()
        return WifiNetwork(
            ssid = result.SSID.takeIf { it.isNotBlank() } ?: "Réseau masqué",
            bssid = result.BSSID ?: "inconnu",
            rssi = result.level,
            frequencyMhz = result.frequency,
            channel = channelFromFrequency(result.frequency),
            security = securityFromCapabilities(caps),
            capabilities = caps
        )
    }

    private fun securityFromCapabilities(caps: String): String {
        val c = caps.uppercase()
        return when {
            "SAE" in c && ("PSK" in c || "WPA2" in c || "RSN" in c) -> "WPA2/WPA3 transition"
            "SAE" in c -> "WPA3-SAE"
            "OWE" in c -> "OWE"
            "EAP_SUITE_B_192" in c -> "WPA3-Enterprise"
            "WPA3" in c -> "WPA3"
            "WPA2" in c || "RSN" in c -> if ("EAP" in c) "WPA2-Enterprise" else "WPA2"
            "WPA" in c -> if ("EAP" in c) "WPA-Enterprise" else "WPA"
            "WEP" in c -> "WEP"
            "WAPI" in c -> "WAPI"
            else -> "Ouvert"
        }
    }

    private fun channelFromFrequency(freq: Int): Int? = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2407) / 5
        freq in 4910..4980 -> (freq - 4000) / 5
        freq in 5000..5895 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5950) / 5
        else -> null
    }
}
