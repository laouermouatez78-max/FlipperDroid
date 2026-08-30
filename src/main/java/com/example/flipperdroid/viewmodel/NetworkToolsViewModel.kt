package com.example.flipperdroid.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.UnknownHostException


data class NetworkResult(
    val command: String,
    val output: String,
    val isError: Boolean = false
)

/** Rootless V5 diagnostics for an explicit user-supplied target. */
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
            addResult("Reachability", "Enter a valid hostname, IPv4 address or IPv6 address.", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val started = System.currentTimeMillis()
                val addresses = InetAddress.getAllByName(target).distinctBy { it.hostAddress }
                val resolvedMs = System.currentTimeMillis() - started
                val address = addresses.firstOrNull() ?: throw UnknownHostException(target)

                val javaReachable = runCatching { address.isReachable(1200) }.getOrDefault(false)
                val pingReachable = if (!javaReachable) systemPing(address.hostAddress ?: target) else false
                val reachable = javaReachable || pingReachable
                val tcpFallback = if (!reachable) firstReachableService(address, listOf(443, 80), 800) else null
                val elapsed = System.currentTimeMillis() - started

                val output = buildString {
                    appendLine("Host: $target")
                    appendLine("Resolved addresses: ${addresses.joinToString { it.hostAddress ?: "?" }}")
                    appendLine("Primary scope: ${addressScope(address)}")
                    appendLine("DNS resolution: ${resolvedMs} ms")
                    when {
                        javaReachable -> appendLine("Reachability: responsive via Java probe (${elapsed} ms total)")
                        pingReachable -> appendLine("Reachability: responsive via Android ping (${elapsed} ms total)")
                        tcpFallback != null -> appendLine("Reachability: TCP service $tcpFallback responded (${elapsed} ms total)")
                        else -> {
                            appendLine("Reachability: no response to available probes")
                            appendLine("Note: ICMP/TCP can be filtered even when a host is online.")
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
            addResult("DNS lookup", "Enter a valid hostname, IPv4 address or IPv6 address.", true)
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
                            appendLine("${index + 1}. ${address.hostAddress} · ${if (address is Inet6Address) "IPv6" else "IPv4"} · ${addressScope(address)}")
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

    /** Compatibility route/configuration check; intentionally not a packet-level traceroute. */
    fun traceroute(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("Route check", "Enter a valid hostname, IPv4 address or IPv6 address.", true)
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

                val transports = buildList {
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("Wi‑Fi")
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("Cellular")
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("Ethernet")
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("VPN")
                }

                val output = buildString {
                    appendLine("Target: $target (${address.hostAddress})")
                    appendLine("Target scope: ${addressScope(address)}")
                    if (active == null || props == null) {
                        appendLine("No active Android network is available.")
                    } else {
                        appendLine("Transport(s): ${transports.ifEmpty { listOf("other") }.joinToString()}")
                        appendLine("Interface: ${props.interfaceName ?: "unknown"}")
                        val gateways = props.routes.mapNotNull { it.gateway?.hostAddress }.distinct()
                        appendLine("Gateway(s): ${gateways.ifEmpty { listOf("not reported") }.joinToString()}")
                        val dns = props.dnsServers.mapNotNull { it.hostAddress }
                        appendLine("DNS server(s): ${dns.ifEmpty { listOf("not reported") }.joinToString()}")
                        appendLine("Local address(es): ${props.linkAddresses.joinToString { it.address.hostAddress ?: "?" }}")
                        appendLine("Internet capability: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true}")
                        appendLine("Validated network: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true}")
                        appendLine("Metered: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true}")
                    }
                    appendLine()
                    appendLine("V5 reports Android's route configuration; it does not pretend to run a packet-level traceroute.")
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

    /** Limited targeted TCP service check for the explicit host entered by the user. */
    fun checkCommonServices(host: String) {
        val target = normalizeHost(host) ?: run {
            addResult("Common services", "Enter a valid hostname, IPv4 address or IPv6 address.", true)
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
                        appendLine("Scope: ${addressScope(address)}")
                        lines.forEach { appendLine(it) }
                        appendLine("Only six common TCP services were checked on this explicit target.")
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

    internal fun normalizeHost(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank() || raw.length > 500 || raw.any { it == '\n' || it == '\r' || it == '\t' }) return null

        val host = when {
            raw.startsWith("[") && raw.contains(']') -> raw.substringAfter('[').substringBefore(']')
            "://" in raw -> runCatching { URI(raw).host }.getOrNull()
            raw.count { it == ':' } >= 2 -> raw.substringBefore('%').ifBlank { raw }
            else -> runCatching { URI("probe://$raw").host }.getOrNull() ?: raw.substringBefore('/')
        }?.trim()?.trimEnd('.')

        if (host.isNullOrBlank() || host.length > 253 || host.any { it.isWhitespace() }) return null
        if (!Regex("^[A-Za-z0-9._:%-]+$").matches(host)) return null
        return host
    }

    private fun firstReachableService(address: InetAddress, ports: List<Int>, timeoutMs: Int): Int? =
        ports.firstOrNull { canConnect(address, it, timeoutMs) }

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

    private fun canConnect(address: InetAddress, port: Int, timeoutMs: Int): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMs)
                socket.isConnected
            }
        }.getOrDefault(false)

    private fun addressScope(address: InetAddress): String = when {
        address.isLoopbackAddress -> "loopback"
        address.isLinkLocalAddress -> "link-local"
        address.isSiteLocalAddress -> "private/site-local"
        address.isMulticastAddress -> "multicast"
        address.isAnyLocalAddress -> "unspecified/local"
        else -> "public/global or provider-routed"
    }

    private fun addResult(command: String, output: String, isError: Boolean = false) {
        _results.value = (_results.value + NetworkResult(command, output.trim(), isError)).takeLast(30)
    }

    private fun safeMessage(error: Throwable): String =
        error.message?.take(180) ?: error.javaClass.simpleName
}
