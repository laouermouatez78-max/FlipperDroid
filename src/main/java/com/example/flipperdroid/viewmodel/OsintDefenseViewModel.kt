package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.util.Locale


data class DefenseOsintResult(
    val title: String,
    val output: String,
    val isError: Boolean = false
)

class OsintDefenseViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<DefenseOsintResult>>(emptyList())
    val results: StateFlow<List<DefenseOsintResult>> = _results

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun analyzeUrlRisk(value: String) {
        val raw = value.trim().take(4000)
        val uri = runCatching { URI(if ("://" in raw) raw else "https://$raw") }.getOrNull()
        if (uri == null || uri.host.isNullOrBlank() || uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) {
            add("URL Risk Analyzer", "Enter a valid HTTP or HTTPS URL.", true)
            return
        }

        val scheme = uri.scheme.lowercase(Locale.ROOT)
        val host = uri.host.lowercase(Locale.ROOT)
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        val unicodeHost = runCatching { IDN.toUnicode(asciiHost) }.getOrDefault(host)
        val signals = mutableListOf<Pair<Int, String>>()

        if (scheme == "http") signals += 2 to "unencrypted HTTP scheme"
        if (!uri.userInfo.isNullOrBlank()) signals += 4 to "embedded user-info/credentials in URL"
        if (asciiHost.contains("xn--", ignoreCase = true)) signals += 2 to "punycode/IDN hostname"
        if (unicodeHost.any { it.code > 127 }) signals += 1 to "non-ASCII Unicode hostname"
        if (looksLikeIp(host)) signals += 2 to "literal IP address used as host"
        if (host.split('.').size > 5) signals += 1 to "deep subdomain nesting"
        if (raw.length > 220) signals += 1 to "unusually long URL"
        if (raw.count { it == '%' } >= 6) signals += 1 to "heavy percent-encoding"
        if (raw.count { it == '@' } >= 1) signals += 1 to "@ character present"
        if (uri.port != -1 && uri.port !in setOf(80, 443)) signals += 1 to "non-standard web port ${uri.port}"

        val queryNames = uri.rawQuery.orEmpty().split('&')
            .mapNotNull { it.substringBefore('=').takeIf(String::isNotBlank) }
            .map { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
            .take(80)
        val sensitiveNames = queryNames.filter {
            it.lowercase(Locale.ROOT) in setOf("token", "access_token", "auth", "password", "passwd", "secret", "key", "apikey", "api_key", "session", "sid")
        }.distinct()
        if (sensitiveNames.isNotEmpty()) signals += 2 to "sensitive-looking query parameter names: ${sensitiveNames.joinToString(", ")}"

        val redirectNames = queryNames.filter {
            it.lowercase(Locale.ROOT) in setOf("url", "uri", "redirect", "redirect_uri", "next", "continue", "return", "returnurl", "callback")
        }.distinct()
        if (redirectNames.isNotEmpty()) signals += 1 to "redirect/navigation parameter names: ${redirectNames.joinToString(", ")}"

        val score = signals.sumOf { it.first }.coerceAtMost(10)
        val level = when {
            score == 0 -> "LOW"
            score <= 2 -> "LOW-MODERATE"
            score <= 5 -> "MODERATE"
            else -> "ELEVATED"
        }

        val output = buildString {
            appendLine("URL: ${uri.toASCIIString()}")
            appendLine("Host: $host")
            appendLine("Unicode host: $unicodeHost")
            appendLine("Scheme: $scheme")
            appendLine("Heuristic score: $score/10 · $level")
            appendLine("Query values displayed: false")
            appendLine()
            appendLine("Signals:")
            if (signals.isEmpty()) appendLine("• no local heuristic flags")
            else signals.forEach { (_, text) -> appendLine("• $text") }
            appendLine()
            appendLine("This score is a local heuristic, not a malware/phishing verdict. Verify reputation and context independently.")
        }
        add("URL Risk · $host", output)
    }

    fun inspectMailDomain(value: String) {
        val domain = normalizeDomain(value)
        if (domain == null) {
            add("Mail Domain Security", "Enter a valid public domain.", true)
            return
        }

        viewModelScope.launch {
            _busy.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val mx = dnsQuery(domain, "MX")
                    val txt = dnsQuery(domain, "TXT")
                    val dmarc = dnsQuery("_dmarc.$domain", "TXT")
                    val mtaSts = dnsQuery("_mta-sts.$domain", "TXT")
                    val tlsRpt = dnsQuery("_smtp._tls.$domain", "TXT")
                    val caa = dnsQuery(domain, "CAA")
                    val spf = txt.answers.filter { it.contains("v=spf1", ignoreCase = true) }
                    val dmarcPolicies = dmarc.answers.filter { it.contains("v=DMARC1", ignoreCase = true) }
                    val mtaPolicies = mtaSts.answers.filter { it.contains("v=STSv1", ignoreCase = true) }
                    val tlsReports = tlsRpt.answers.filter { it.contains("v=TLSRPTv1", ignoreCase = true) }

                    val scoreParts = listOf(
                        mx.answers.isNotEmpty(),
                        spf.isNotEmpty(),
                        dmarcPolicies.isNotEmpty(),
                        mtaPolicies.isNotEmpty(),
                        tlsReports.isNotEmpty(),
                        caa.answers.isNotEmpty()
                    )
                    val posture = scoreParts.count { it }
                    val dnssecObserved = listOf(mx, txt, dmarc, mtaSts, tlsRpt, caa).any { it.authenticatedData }

                    val output = buildString {
                        appendLine("Domain: $domain")
                        appendLine("Mail-security posture signals: $posture/6")
                        appendLine("DNSSEC authenticated-data observed: $dnssecObserved")
                        appendLine()
                        appendLine("MX (${mx.answers.size}):")
                        if (mx.answers.isEmpty()) appendLine("• none returned") else mx.answers.take(12).forEach { appendLine("• $it") }
                        appendLine("SPF (${spf.size}):")
                        if (spf.isEmpty()) appendLine("• not observed") else spf.take(5).forEach { appendLine("• $it") }
                        appendLine("DMARC (${dmarcPolicies.size}):")
                        if (dmarcPolicies.isEmpty()) appendLine("• not observed") else dmarcPolicies.take(5).forEach { appendLine("• $it") }
                        appendLine("MTA-STS DNS (${mtaPolicies.size}):")
                        if (mtaPolicies.isEmpty()) appendLine("• not observed") else mtaPolicies.take(5).forEach { appendLine("• $it") }
                        appendLine("TLS-RPT (${tlsReports.size}):")
                        if (tlsReports.isEmpty()) appendLine("• not observed") else tlsReports.take(5).forEach { appendLine("• $it") }
                        appendLine("CAA (${caa.answers.size}):")
                        if (caa.answers.isEmpty()) appendLine("• none returned") else caa.answers.take(10).forEach { appendLine("• $it") }
                        appendLine()
                        appendLine("Absence here means 'not observed through this DNS query', not necessarily a security failure. Policies should be reviewed in context.")
                    }
                    DefenseOsintResult("Mail Domain Security · $domain", output.trim())
                }.getOrElse {
                    DefenseOsintResult("Mail Domain Security · $domain", "Public DNS security check failed: ${it.message ?: it::class.java.simpleName}", true)
                }
            }
            append(result)
            _busy.value = false
        }
    }

    fun clear() {
        _results.value = emptyList()
    }

    fun reportText(): String = buildString {
        appendLine("FlipperDroid V5 — Domain & URL Defense Report")
        appendLine("Local heuristics and public DNS observations; verify findings independently.")
        appendLine("=".repeat(56))
        _results.value.forEachIndexed { index, result ->
            appendLine()
            appendLine("${index + 1}. ${result.title}")
            appendLine("-".repeat(56))
            appendLine(result.output)
        }
    }

    private data class DnsAnswer(val answers: List<String>, val authenticatedData: Boolean)

    private fun dnsQuery(name: String, type: String): DnsAnswer {
        val endpoint = "https://dns.google/resolve?name=${encode(name)}&type=${encode(type)}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/dns-json, application/json")
            setRequestProperty("User-Agent", "FlipperDroid-V5-OSINT/5.0")
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText().take(800_000) }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            val json = JSONObject(body)
            val array = json.optJSONArray("Answer") ?: JSONArray()
            val answers = buildList {
                for (i in 0 until array.length()) {
                    val data = array.optJSONObject(i)?.optString("data")?.trim()?.trimEnd('.').orEmpty()
                    if (data.isNotBlank()) add(data)
                }
            }.distinct()
            return DnsAnswer(answers, json.optBoolean("AD", false))
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeDomain(value: String): String? {
        val raw = value.trim().lowercase(Locale.ROOT)
        if (raw.isBlank() || raw.length > 500 || raw.any { it.isWhitespace() }) return null
        val uri = runCatching { URI(if ("://" in raw) raw else "https://$raw") }.getOrNull() ?: return null
        val host = uri.host?.trimEnd('.')?.lowercase(Locale.ROOT) ?: return null
        if (!host.contains('.') || host.length > 253 || looksLikeIp(host)) return null
        val ascii = runCatching { IDN.toASCII(host) }.getOrNull() ?: return null
        return ascii.lowercase(Locale.ROOT)
    }

    private fun looksLikeIp(value: String): Boolean {
        val v4 = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(value)
        val v6 = value.contains(':') && Regex("^[0-9A-Fa-f:.%]+$").matches(value)
        if (!v4 && !v6) return false
        return runCatching { InetAddress.getByName(value) }.isSuccess
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun add(title: String, output: String, isError: Boolean = false) {
        append(DefenseOsintResult(title, output.trim(), isError))
    }

    private fun append(result: DefenseOsintResult) {
        _results.value = (_results.value + result).takeLast(25)
    }
}
