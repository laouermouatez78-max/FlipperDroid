package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress
import java.net.URI


data class OsintResult(
    val title: String,
    val output: String,
    val isError: Boolean = false
)

class OsintViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<OsintResult>>(emptyList())
    val results: StateFlow<List<OsintResult>> = _results

    fun inspectDomain(value: String) {
        val raw = value.trim().lowercase()
        if (raw.isBlank() || raw.length > 253 || raw.any { it.isWhitespace() }) {
            addResult("Domain Inspector", "Enter a valid domain name.", true)
            return
        }

        val host = runCatching {
            val uri = URI(if ("://" in raw) raw else "https://$raw")
            uri.host
        }.getOrNull()?.trimEnd('.')

        if (host.isNullOrBlank()) {
            addResult("Domain Inspector", "Unable to parse this domain.", true)
            return
        }

        val labels = host.split('.').filter { it.isNotBlank() }
        val tld = labels.lastOrNull().orEmpty()
        val registrableHint = if (labels.size >= 2) labels.takeLast(2).joinToString(".") else host

        val output = buildString {
            appendLine("Host: $host")
            appendLine("Labels: ${labels.size}")
            appendLine("TLD / suffix hint: .$tld")
            appendLine("Registrable-domain hint: $registrableHint")
            appendLine("Internationalized form: ${runCatching { java.net.IDN.toASCII(host) }.getOrDefault(host)}")
        }
        addResult("Domain Inspector · $host", output)
    }

    fun inspectIp(value: String) {
        val candidate = value.trim()
        if (!looksLikeIp(candidate)) {
            addResult("IP Inspector", "Enter an IPv4 or IPv6 address.", true)
            return
        }

        val address = runCatching { InetAddress.getByName(candidate) }.getOrNull()
        if (address == null) {
            addResult("IP Inspector", "Unable to parse this IP address.", true)
            return
        }

        val scope = when {
            address.isLoopbackAddress -> "loopback"
            address.isLinkLocalAddress -> "link-local"
            address.isSiteLocalAddress -> "private/site-local"
            address.isMulticastAddress -> "multicast"
            address.isAnyLocalAddress -> "unspecified/local"
            else -> "public/global or provider-routed"
        }

        val output = buildString {
            appendLine("Address: ${address.hostAddress}")
            appendLine("Family: ${if (candidate.contains(':')) "IPv6" else "IPv4"}")
            appendLine("Scope: $scope")
            appendLine("Loopback: ${address.isLoopbackAddress}")
            appendLine("Link-local: ${address.isLinkLocalAddress}")
            appendLine("Private/site-local: ${address.isSiteLocalAddress}")
            appendLine("Multicast: ${address.isMulticastAddress}")
        }
        addResult("IP Inspector · ${address.hostAddress}", output)
    }

    fun inspectUrl(value: String) {
        val raw = value.trim()
        if (raw.isBlank()) {
            addResult("URL Inspector", "Enter a URL.", true)
            return
        }

        val uri = runCatching {
            URI(if ("://" in raw) raw else "https://$raw")
        }.getOrNull()

        if (uri == null || uri.host.isNullOrBlank() || uri.scheme !in setOf("http", "https")) {
            addResult("URL Inspector", "Enter a valid HTTP or HTTPS URL.", true)
            return
        }

        val defaultPort = if (uri.scheme == "https") 443 else 80
        val output = buildString {
            appendLine("Scheme: ${uri.scheme}")
            appendLine("Host: ${uri.host}")
            appendLine("Port: ${if (uri.port == -1) defaultPort else uri.port}")
            appendLine("Path: ${uri.rawPath?.ifBlank { "/" } ?: "/"}")
            appendLine("Query present: ${!uri.rawQuery.isNullOrBlank()}")
            appendLine("Fragment present: ${!uri.rawFragment.isNullOrBlank()}")
            appendLine("HTTPS: ${uri.scheme == "https"}")
            if (!uri.userInfo.isNullOrBlank()) appendLine("Warning: URL contains user-info credentials")
        }
        addResult("URL Inspector · ${uri.host}", output)
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    private fun addResult(title: String, output: String, isError: Boolean = false) {
        _results.value = (_results.value + OsintResult(title, output.trim(), isError)).takeLast(20)
    }

    private fun looksLikeIp(value: String): Boolean =
        Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(value) || value.contains(':')
}
