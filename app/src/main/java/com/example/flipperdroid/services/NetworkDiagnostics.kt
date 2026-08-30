package com.example.flipperdroid.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.flipperdroid.model.NetworkSnapshot
import com.example.flipperdroid.model.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkDiagnostics(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun snapshot(): NetworkSnapshot {
        val network = cm.activeNetwork
        val caps = network?.let(cm::getNetworkCapabilities)
        val props = network?.let(cm::getLinkProperties)
        val transport = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi‑Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellulaire"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
            else -> "Aucun / autre"
        }
        return NetworkSnapshot(
            transport = transport,
            interfaceName = props?.interfaceName,
            addresses = props?.linkAddresses?.map { it.address.hostAddress ?: it.toString() }.orEmpty(),
            dnsServers = props?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty(),
            gateways = props?.routes
                ?.filter { it.hasGateway() }
                ?.mapNotNull { it.gateway?.hostAddress }
                ?.distinct()
                .orEmpty(),
            validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = cm.isActiveNetworkMetered
        )
    }

    suspend fun resolve(host: String): ProbeResult = withContext(Dispatchers.IO) {
        val clean = host.trim()
        if (clean.isBlank()) return@withContext ProbeResult(clean, false, null, "Hôte vide")
        val start = System.nanoTime()
        runCatching {
            val addresses = InetAddress.getAllByName(clean).mapNotNull { it.hostAddress }.distinct()
            val latency = (System.nanoTime() - start) / 1_000_000
            ProbeResult(clean, true, latency, addresses.joinToString())
        }.getOrElse {
            ProbeResult(clean, false, null, "Résolution impossible: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    suspend fun tcpProbe(host: String, port: Int, timeoutMs: Int = 2500): ProbeResult = withContext(Dispatchers.IO) {
        val clean = host.trim()
        if (clean.isBlank() || port !in 1..65535) {
            return@withContext ProbeResult("$clean:$port", false, null, "Hôte ou port invalide")
        }
        val start = System.nanoTime()
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(clean, port), timeoutMs)
            }
            val latency = (System.nanoTime() - start) / 1_000_000
            ProbeResult("$clean:$port", true, latency, "Connexion TCP réussie")
        }.getOrElse {
            ProbeResult("$clean:$port", false, null, "Échec: ${it.javaClass.simpleName} — ${it.message ?: "sans détail"}")
        }
    }

    /**
     * Probes several ports on a single, explicitly provided host — still one target,
     * never an automated subnet/host sweep. Useful to check which of a handful of
     * expected services (e.g. 22, 80, 443) are reachable in one action instead of
     * repeating the single-port probe manually. Capped to a small port list.
     */
    suspend fun tcpProbeMultiple(host: String, ports: List<Int>, timeoutMs: Int = 1500): List<ProbeResult> =
        withContext(Dispatchers.IO) {
            ports.take(16).distinct().map { port -> tcpProbe(host, port, timeoutMs) }
        }
}
