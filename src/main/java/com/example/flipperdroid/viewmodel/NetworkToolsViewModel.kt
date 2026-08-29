package com.example.flipperdroid.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException


data class NetworkResult(
    val command: String,
    val output: String,
    val isError: Boolean = false
)

/**
 * Rootless V4 network diagnostics.
 *
 * This ViewModel intentionally avoids the legacy bundled Nmap + `su` workflow, which
 * fails on normal non-rooted Android devices. All checks use standard Android/Java
 * networking APIs and are intended for hosts the user owns or is authorized to test.
 */
class NetworkToolsViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<NetworkResult>>(emptyList())
    val results: StateFlow<List<NetworkResult>> = _results

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun ping(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("Reachability", "Enter a valid hostname or IP address.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val started = System.currentTimeMillis()
                val address = InetAddress.getByName(target)
                val resolvedMs = System.currentTimeMillis() - started

                val reachable = runCatching { address.isReachable(1500) }.getOrDefault(false)
                val tcpFallback = if (!reachable) firstReachableService(address, listOf(443, 80), 800) else null
                val elapsed = System.currentTimeMillis() - started

                val output = buildString {
                    appendLine("Host: $target")
                    appendLine("Address: ${address.hostAddress}")
                    appendLine("DNS resolution: ${resolvedMs} ms")
                    when {
                        reachable -> appendLine("Reachability: responsive (${elapsed} ms total)")
                        tcpFallback != null -> appendLine("Reachability: TCP service $tcpFallback responded (${elapsed} ms total)")
                        else -> {
                            appendLine("Reachability: no response to the available probes")
                            appendLine("Note: Android/network firewalls can block ICMP even when a host is online.")
                        }
                    }
                }
                addResult("Reachability $target", output, !reachable && tcpFallback == null)
            } catch (_: UnknownHostException) {
                addResult("Reachability $target", "Host could not be resolved.", true)
            } catch (e: Exception) {
                addResult("Reachability $target", "Error: ${safeMessage(e)}", true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun dnsLookup(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("DNS lookup", "Enter a valid hostname or IP address.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val started = System.currentTimeMillis()
                val addresses = InetAddress.getAllByName(target).distinctBy { it.hostAddress }
                val elapsed = System.currentTimeMillis() - started
                val output = buildString {
                    appendLine("DNS lookup: $target")
                    appendLine("Completed in ${elapsed} ms")
                    if (addresses.isEmpty()) {
                        appendLine("No address returned.")
                    } else {
                        addresses.forEachIndexed { index, address ->
                            appendLine("${index + 1}. ${address.hostAddress}")
                        }
                    }
                }
                addResult("DNS lookup $target", output, addresses.isEmpty())
            } catch (_: UnknownHostException) {
                addResult("DNS lookup $target", "Host not found: $target", true)
            } catch (e: Exception) {
                addResult("DNS lookup $target", "Error: ${safeMessage(e)}", true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Compatibility name retained for the existing UI. This is a route/configuration
     * check, not a packet-level traceroute.
     */
    fun traceroute(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("Route check", "Enter a valid hostname or IP address.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val address = InetAddress.getByName(target)
                val context = appContext
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val active = cm?.activeNetwork
                val props = active?.let { cm.getLinkProperties(it) }
                val caps = active?.let { cm.getNetworkCapabilities(it) }

                val output = buildString {
                    appendLine("Target: $target (${address.hostAddress})")
                    if (active == null || props == null) {
                        appendLine("No active Android network is available.")
                    } else {
                        appendLine("Interface: ${props.interfaceName ?: "unknown"}")
                        val gateways = props.routes.mapNotNull { it.gateway?.hostAddress }.distinct()
                        appendLine("Gateway(s): ${gateways.ifEmpty { listOf("not reported") }.joinToString()}")
                        val dns = props.dnsServers.mapNotNull { it.hostAddress }
                        appendLine("DNS server(s): ${dns.ifEmpty { listOf("not reported") }.joinToString()}")
                        appendLine("Local address(es): ${props.linkAddresses.joinToString { it.address.hostAddress ?: "?" }}")
                        appendLine("Internet capability: ${caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true}")
                        appendLine("Validated network: ${caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true}")
                    }
                    appendLine()
                    appendLine("This V4 check reports Android's active route configuration; it does not spoof a traceroute.")
                }
                addResult("Route check $target", output, active == null)
            } catch (_: UnknownHostException) {
                addResult("Route check $target", "Host could not be resolved.", true)
            } catch (e: Exception) {
                addResult("Route check $target", "Error: ${safeMessage(e)}", true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    /** Limited, targeted service check for the explicit host entered by the user. */
    fun checkCommonServices(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("Common services", "Enter a valid hostname or IP address.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val address = InetAddress.getByName(target)
                val ports = listOf(22 to "SSH", 53 to "DNS", 80 to "HTTP", 443 to "HTTPS", 445 to "SMB", 8080 to "HTTP-alt")
                val lines = ports.map { (port, label) ->
                    val open = canConnect(address, port, 450)
                    "$port/$label: ${if (open) "reachable" else "no response"}"
                }
                addResult(
                    "Common services $target",
                    buildString {
                        appendLine("Target: $target (${address.hostAddress})")
                        lines.forEach { appendLine(it) }
                        appendLine("Only six common TCP services were checked.")
                    }
                )
            } catch (_: UnknownHostException) {
                addResult("Common services $target", "Host could not be resolved.", true)
            } catch (e: Exception) {
                addResult("Common services $target", "Error: ${safeMessage(e)}", true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    private fun normalizeHost(value: String): String? {
        val host = value.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore(':')
            .trim()
        if (host.isBlank() || host.length > 253 || host.any { it.isWhitespace() }) return null
        return host
    }

    private fun firstReachableService(address: InetAddress, ports: List<Int>, timeoutMs: Int): Int? =
        ports.firstOrNull { canConnect(address, it, timeoutMs) }

    private fun canConnect(address: InetAddress, port: Int, timeoutMs: Int): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMs)
                socket.isConnected
            }
        }.getOrDefault(false)

    private fun addResult(command: String, output: String, isError: Boolean = false) {
        _results.value = (_results.value + NetworkResult(command, output.trim(), isError)).takeLast(30)
    }

    private fun safeMessage(error: Throwable): String =
        error.message?.take(180) ?: error.javaClass.simpleName
}
