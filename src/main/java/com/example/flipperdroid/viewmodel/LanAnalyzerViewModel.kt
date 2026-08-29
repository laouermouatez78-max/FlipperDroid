package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections


data class LanHost(
    val ip: String,
    val hostname: String?,
    val openPorts: List<Int>,
    val reachable: Boolean
)

class LanAnalyzerViewModel : ViewModel() {
    private val _localIp = MutableStateFlow(findLocalIpv4())
    val localIp: StateFlow<String?> = _localIp

    private val _hosts = MutableStateFlow<List<LanHost>>(emptyList())
    val hosts: StateFlow<List<LanHost>> = _hosts

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val commonPorts = listOf(22, 53, 80, 443, 445, 554, 8000, 8080)

    fun refreshLocalIp() {
        _localIp.value = findLocalIpv4()
    }

    fun scanLocalSubnet() {
        val ip = _localIp.value ?: run {
            _status.value = "No private IPv4 address found. Connect to your Wi‑Fi first."
            return
        }
        val parts = ip.split('.')
        if (parts.size != 4) return
        val prefix = parts.take(3).joinToString(".")

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _hosts.value = emptyList()
            _status.value = "Scanning $prefix.0/24…"
            try {
                val semaphore = Semaphore(32)
                val found = coroutineScope {
                    (1..254).map { last ->
                        async {
                            semaphore.withPermit {
                                probeHost("$prefix.$last")
                            }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }
                }
                _hosts.value = found
                _status.value = "Scan complete · ${found.size} active host(s)"
            } catch (e: Exception) {
                _status.value = "LAN scan error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun probeHost(ip: String): LanHost? {
        val address = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return null
        val reachable = runCatching { address.isReachable(250) }.getOrDefault(false)
        val ports = commonPorts.filter { port -> isPortOpen(ip, port, 140) }
        if (!reachable && ports.isEmpty()) return null
        val hostname = runCatching {
            val canonical = address.canonicalHostName
            canonical.takeIf { it != ip }
        }.getOrNull()
        return LanHost(ip, hostname, ports, true)
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.isConnected
        }
    }.getOrDefault(false)

    companion object {
        private fun findLocalIpv4(): String? = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { Collections.list(it.inetAddresses).asSequence() }
                .mapNotNull { it.hostAddress }
                .firstOrNull { address ->
                    !address.contains(':') && (
                        address.startsWith("10.") ||
                        address.startsWith("192.168.") ||
                        Regex("^172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(address)
                    )
                }
        }.getOrNull()
    }
}
