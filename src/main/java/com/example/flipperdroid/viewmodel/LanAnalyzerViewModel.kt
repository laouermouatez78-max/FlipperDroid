package com.example.flipperdroid.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger


data class LanHost(
    val ip: String,
    val hostname: String?,
    val openPorts: List<Int>,
    val reachable: Boolean
)

class LanAnalyzerViewModel(app: Application) : AndroidViewModel(app) {
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp

    private val _networkType = MutableStateFlow("No private Wi‑Fi/Ethernet detected")
    val networkType: StateFlow<String> = _networkType

    private val _hosts = MutableStateFlow<List<LanHost>>(emptyList())
    val hosts: StateFlow<List<LanHost>> = _hosts

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount

    private val commonPorts = listOf(22, 53, 80, 443, 445, 8080)
    private var scanJob: Job? = null

    init {
        refreshLocalIp()
    }

    fun refreshLocalIp() {
        val active = connectivity?.activeNetwork
        val caps = active?.let { connectivity.getNetworkCapabilities(it) }
        val props = active?.let { connectivity.getLinkProperties(it) }
        val eligible = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

        val ip = if (eligible) {
            props?.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.filter { it.address.size == 4 }
                ?.mapNotNull { it.hostAddress }
                ?.firstOrNull(::isPrivateIpv4)
        } else null

        _localIp.value = ip
        _networkType.value = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi‑Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "No private Wi‑Fi/Ethernet detected"
        }
        if (!_isScanning.value) {
            _status.value = if (ip != null) "Private LAN ready" else "Connect to a private Wi‑Fi/Ethernet network first"
        }
    }

    fun scanLocalSubnet() {
        if (_isScanning.value) return
        refreshLocalIp()
        val ip = _localIp.value ?: run {
            _status.value = "No private IPv4 address found on Wi‑Fi/Ethernet"
            return
        }
        val parts = ip.split('.')
        if (parts.size != 4) {
            _status.value = "Unsupported local IPv4 format"
            return
        }
        val prefix = parts.take(3).joinToString(".")

        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _hosts.value = emptyList()
            _progress.value = 0f
            _scannedCount.value = 0
            _status.value = "Scanning your private $prefix.0/24…"
            val completed = AtomicInteger(0)
            try {
                val semaphore = Semaphore(16)
                coroutineScope {
                    (1..254).map { last ->
                        async {
                            semaphore.withPermit {
                                val host = probeHost("$prefix.$last")
                                if (host != null) {
                                    _hosts.update { previous ->
                                        (previous + host)
                                            .distinctBy { it.ip }
                                            .sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }
                                    }
                                }
                                val done = completed.incrementAndGet()
                                _scannedCount.value = done
                                _progress.value = done / 254f
                                _status.value = "Scanning $prefix.0/24 · $done/254 · ${_hosts.value.size} host(s) found"
                                host
                            }
                        }
                    }.awaitAll()
                }
                _status.value = "Scan complete · ${_hosts.value.size} responsive host(s)"
                _progress.value = 1f
            } catch (e: kotlinx.coroutines.CancellationException) {
                _status.value = "LAN scan stopped · ${_scannedCount.value}/254 checked · ${_hosts.value.size} host(s) kept"
                throw e
            } catch (e: Exception) {
                _status.value = "LAN scan error: ${e.message?.take(160) ?: e.javaClass.simpleName}"
            } finally {
                _isScanning.value = false
                scanJob = null
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        _status.value = "LAN scan stopped · ${_scannedCount.value}/254 checked · ${_hosts.value.size} host(s) kept"
    }

    private fun probeHost(ip: String): LanHost? {
        val address = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return null
        val reachable = runCatching { address.isReachable(300) }.getOrDefault(false) || systemPing(ip)
        val ports = commonPorts.filter { port -> isPortOpen(ip, port, 180) }
        if (!reachable && ports.isEmpty()) return null

        return LanHost(
            ip = ip,
            hostname = null,
            openPorts = ports,
            reachable = true
        )
    }

    private fun systemPing(host: String): Boolean = runCatching {
        val process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "1", host)
            .redirectErrorStream(true)
            .start()
        try {
            process.waitFor() == 0
        } finally {
            runCatching { process.destroy() }
        }
    }.getOrDefault(false)

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.isConnected
        }
    }.getOrDefault(false)

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }

    companion object {
        internal fun isPrivateIpv4(address: String): Boolean =
            address.startsWith("10.") ||
                address.startsWith("192.168.") ||
                Regex("^172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(address)
    }
}
