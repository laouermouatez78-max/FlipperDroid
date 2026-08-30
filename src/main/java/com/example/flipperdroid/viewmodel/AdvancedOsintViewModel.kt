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
import java.security.MessageDigest
import java.util.Locale


data class AdvancedOsintResult(
    val title: String,
    val output: String,
    val isError: Boolean = false
)

class AdvancedOsintViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<AdvancedOsintResult>>(emptyList())
    val results: StateFlow<List<AdvancedOsintResult>> = _results

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun queryDnsRecords(value: String) {
        val host = normalizeHost(value)
        if (host == null) {
            add("DNS Records", "Enter a valid public hostname.", true)
            return
        }

        viewModelScope.launch {
            _busy.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val types = listOf("A", "AAAA", "MX", "NS", "TXT", "CAA")
                    val sections = types.map { type ->
                        type to dnsGoogle(host, type)
                    }
                    val output = buildString {
                        appendLine("Host: $host")
                        appendLine("Resolver: Google Public DNS over HTTPS")
                        appendLine()
                        sections.forEach { (type, records) ->
                            appendLine("$type records (${records.size}):")
                            if (records.isEmpty()) appendLine("• none returned")
                            else records.take(20).forEach { appendLine("• $it") }
                            appendLine()
                        }
                    }
                    AdvancedOsintResult("DNS Record Intelligence · $host", output.trim())
                }.getOrElse {
                    AdvancedOsintResult("DNS Record Intelligence · $host", "DNS record lookup failed: ${friendlyError(it)}", true)
                }
            }
            append(result)
            _busy.value = false
        }
    }

    fun queryRdap(value: String) {
        val raw = value.trim()
        if (raw.isBlank()) {
            add("RDAP Registry", "Enter a public domain or public IP address.", true)
            return
        }

        viewModelScope.launch {
            _busy.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val ip = parseIpLiteral(raw)
                    if (ip != null && isPrivateOrSpecial(ip)) {
                        return@runCatching AdvancedOsintResult(
                            "RDAP Registry",
                            "Private/local/special-use IP addresses are not sent to public RDAP services.",
                            true
                        )
                    }

                    val host = if (ip == null) normalizeHost(raw) else null
                    if (ip == null && host == null) {
                        return@runCatching AdvancedOsintResult("RDAP Registry", "Unable to parse a public domain or IP address.", true)
                    }

                    val endpoint = if (ip != null) {
                        "https://rdap.org/ip/${urlEncode(ip.hostAddress ?: raw)}"
                    } else {
                        "https://rdap.org/domain/${urlEncode(host!!)}"
                    }
                    val json = httpJson(endpoint)
                    val output = if (ip != null) summarizeRdapIp(json) else summarizeRdapDomain(json)
                    AdvancedOsintResult("RDAP Registry · ${ip?.hostAddress ?: host}", output)
                }.getOrElse {
                    AdvancedOsintResult("RDAP Registry · $raw", "RDAP lookup failed: ${friendlyError(it)}", true)
                }
            }
            append(result)
            _busy.value = false
        }
    }

    fun generateDomainVariants(value: String) {
        val host = normalizeHost(value)
        if (host == null) {
            add("Domain Similarity", "Enter a valid domain.", true)
            return
        }
        val labels = host.split('.')
        if (labels.size < 2) {
            add("Domain Similarity", "A registrable-looking domain is required.", true)
            return
        }

        val suffix = labels.drop(1).joinToString(".")
        val base = labels.first()
        if (base.length < 3) {
            add("Domain Similarity", "Domain label is too short for useful variants.", true)
            return
        }

        val variants = linkedSetOf<String>()
        for (i in base.indices) {
            if (base.length > 3) variants += base.removeRange(i, i + 1) + "." + suffix
            if (i < base.lastIndex) {
                val chars = base.toCharArray()
                val tmp = chars[i]
                chars[i] = chars[i + 1]
                chars[i + 1] = tmp
                variants += String(chars) + "." + suffix
            }
        }
        for (i in 1 until base.length) variants += base.substring(0, i) + "-" + base.substring(i) + "." + suffix
        variants += "www-$base.$suffix"
        variants += "$base-secure.$suffix"
        variants += "$base-login.$suffix"
        variants += "$base-support.$suffix"

        val digitMap = mapOf('o' to '0', 'i' to '1', 'l' to '1', 'e' to '3', 'a' to '4', 's' to '5')
        base.forEachIndexed { index, c ->
            val replacement = digitMap[c]
            if (replacement != null) {
                val chars = base.toCharArray()
                chars[index] = replacement
                variants += String(chars) + "." + suffix
            }
        }

        val output = buildString {
            appendLine("Reference: $host")
            appendLine("Generated locally: ${variants.size} similarity candidates")
            appendLine()
            variants.take(60).forEach { appendLine("• $it") }
            appendLine()
            appendLine("These are heuristic candidates only. No registration or availability checks were performed.")
        }
        add("Domain Similarity / Typosquatting · $host", output)
    }

    fun extractIndicators(text: String) {
        val source = text.take(100_000)
        if (source.isBlank()) {
            add("IOC Extractor", "Paste text, logs, headers or notes to parse locally.", true)
            return
        }

        val urls = Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE)
            .findAll(source).map { it.value.trimEnd('.', ',', ';', ')', ']') }.distinct().take(80).toList()
        val emails = Regex("(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}")
            .findAll(source).map { it.value }.distinct().take(80).toList()
        val ipv4 = Regex("(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d)")
            .findAll(source).map { it.value }.filter { validIpv4(it) }.distinct().take(80).toList()
        val sha256 = Regex("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])")
            .findAll(source).map { it.value.lowercase(Locale.ROOT) }.distinct().take(40).toList()
        val sha1 = Regex("(?i)(?<![0-9a-f])[0-9a-f]{40}(?![0-9a-f])")
            .findAll(source).map { it.value.lowercase(Locale.ROOT) }.distinct().take(40).toList()
        val md5 = Regex("(?i)(?<![0-9a-f])[0-9a-f]{32}(?![0-9a-f])")
            .findAll(source).map { it.value.lowercase(Locale.ROOT) }.distinct().take(40).toList()
        val domains = Regex("(?i)(?<![A-Z0-9_-])(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\\.)+[A-Z]{2,63}(?![A-Z0-9_-])")
            .findAll(source).map { it.value.lowercase(Locale.ROOT) }
            .filterNot { domain -> emails.any { it.endsWith("@$domain", ignoreCase = true) } }
            .distinct().take(100).toList()

        val output = buildString {
            appendLine("Local IOC extraction — no network requests")
            appendLine("Input characters: ${source.length}")
            appendLine("URLs: ${urls.size}")
            urls.forEach { appendLine("• $it") }
            appendLine("Domains: ${domains.size}")
            domains.forEach { appendLine("• $it") }
            appendLine("IPv4: ${ipv4.size}")
            ipv4.forEach { appendLine("• $it") }
            appendLine("Emails: ${emails.size}")
            emails.forEach { appendLine("• $it") }
            appendLine("SHA-256: ${sha256.size}")
            sha256.forEach { appendLine("• $it") }
            appendLine("SHA-1: ${sha1.size}")
            sha1.forEach { appendLine("• $it") }
            appendLine("MD5: ${md5.size}")
            md5.forEach { appendLine("• $it") }
        }
        add("IOC Extractor", output)
    }

    fun analyzeEmailHeaders(value: String) {
        val raw = value.take(100_000)
        if (raw.isBlank()) {
            add("Email Header Analyzer", "Paste raw email headers.", true)
            return
        }

        val unfolded = raw.replace(Regex("\\r?\\n[ \\t]+"), " ")
        fun headers(name: String): List<String> = Regex("(?im)^${Regex.escape(name)}:\\s*(.+)$")
            .findAll(unfolded).map { it.groupValues[1].trim() }.toList()

        val from = headers("From").firstOrNull()
        val replyTo = headers("Reply-To").firstOrNull()
        val returnPath = headers("Return-Path").firstOrNull()
        val messageId = headers("Message-ID").firstOrNull()
        val received = headers("Received")
        val auth = headers("Authentication-Results") + headers("ARC-Authentication-Results")

        val authJoined = auth.joinToString(" ").lowercase(Locale.ROOT)
        val spf = authToken(authJoined, "spf")
        val dkim = authToken(authJoined, "dkim")
        val dmarc = authToken(authJoined, "dmarc")
        val messageIdDomain = messageId?.substringAfterLast('@', "")?.trim('>', ' ', '\t')?.takeIf { it.contains('.') }
        val receivedIps = received.flatMap { line ->
            Regex("(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d)").findAll(line).map { it.value }.filter { validIpv4(it) }.toList()
        }.distinct().take(20)

        val output = buildString {
            appendLine("Header lines: ${raw.lines().size}")
            appendLine("Received hops observed: ${received.size}")
            appendLine("From: ${from ?: "not present"}")
            appendLine("Reply-To: ${replyTo ?: "not present"}")
            appendLine("Return-Path: ${returnPath ?: "not present"}")
            appendLine("Message-ID domain: ${messageIdDomain ?: "not available"}")
            appendLine("SPF result hint: $spf")
            appendLine("DKIM result hint: $dkim")
            appendLine("DMARC result hint: $dmarc")
            appendLine()
            appendLine("IPv4 addresses observed in Received headers (${receivedIps.size}):")
            if (receivedIps.isEmpty()) appendLine("• none") else receivedIps.forEach { appendLine("• $it") }
            appendLine()
            appendLine("Authentication results are reported as declared by the message headers; they are not independently verified here.")
        }
        add("Email Header Analyzer", output)
    }

    fun fingerprintText(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty()) {
            add("Text Fingerprint", "Enter text to fingerprint locally.", true)
            return
        }
        val sha256 = digest("SHA-256", bytes)
        val sha1 = digest("SHA-1", bytes)
        val lines = value.lines().size
        val uniqueChars = value.toSet().size
        val output = buildString {
            appendLine("Bytes (UTF-8): ${bytes.size}")
            appendLine("Characters: ${value.length}")
            appendLine("Lines: $lines")
            appendLine("Unique characters: $uniqueChars")
            appendLine("SHA-256: $sha256")
            appendLine("SHA-1: $sha1")
        }
        add("Local Text Fingerprint", output)
    }

    fun clear() {
        _results.value = emptyList()
    }

    fun reportText(): String = buildString {
        appendLine("FlipperDroid V5 — Advanced OSINT Report")
        appendLine("Generated from locally displayed results. Verify findings independently.")
        appendLine("=".repeat(54))
        _results.value.forEachIndexed { index, result ->
            appendLine()
            appendLine("${index + 1}. ${result.title}")
            appendLine("-".repeat(54))
            appendLine(result.output)
        }
    }

    private fun dnsGoogle(host: String, type: String): List<String> {
        val endpoint = "https://dns.google/resolve?name=${urlEncode(host)}&type=${urlEncode(type)}"
        val json = httpJson(endpoint)
        val answer = json.optJSONArray("Answer") ?: JSONArray()
        val values = mutableListOf<String>()
        for (i in 0 until answer.length()) {
            val item = answer.optJSONObject(i) ?: continue
            val data = item.optString("data").trim().trimEnd('.')
            if (data.isNotBlank()) values += data
        }
        return values.distinct()
    }

    private fun httpJson(endpoint: String): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/rdap+json, application/dns-json, application/json")
            setRequestProperty("User-Agent", "FlipperDroid-V5-OSINT/5.0")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText().take(1_000_000) }.orEmpty()
            if (code !in 200..299) error("HTTP $code")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun summarizeRdapDomain(json: JSONObject): String = buildString {
        appendLine("Object class: ${json.optString("objectClassName", "domain")}")
        appendLine("Handle: ${json.optString("handle", "not disclosed")}")
        appendLine("LDH name: ${json.optString("ldhName", "not disclosed")}")
        appendLine("Unicode name: ${json.optString("unicodeName", "not disclosed")}")
        appendLine("Status: ${jsonArrayStrings(json.optJSONArray("status")).joinToString(", ").ifBlank { "not disclosed" }}")
        appendLine()
        appendLine("Events:")
        val events = json.optJSONArray("events") ?: JSONArray()
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            appendLine("• ${event.optString("eventAction", "event")}: ${event.optString("eventDate", "unknown")}")
        }
        val ns = json.optJSONArray("nameservers") ?: JSONArray()
        if (ns.length() > 0) {
            appendLine("Nameservers:")
            for (i in 0 until ns.length()) {
                val name = ns.optJSONObject(i)?.optString("ldhName").orEmpty()
                if (name.isNotBlank()) appendLine("• $name")
            }
        }
        appendLine()
        appendLine("RDAP data is registry-supplied public registration metadata; field availability varies by registry.")
    }

    private fun summarizeRdapIp(json: JSONObject): String = buildString {
        appendLine("Object class: ${json.optString("objectClassName", "ip network")}")
        appendLine("Name: ${json.optString("name", "not disclosed")}")
        appendLine("Handle: ${json.optString("handle", "not disclosed")}")
        appendLine("Start address: ${json.optString("startAddress", "not disclosed")}")
        appendLine("End address: ${json.optString("endAddress", "not disclosed")}")
        appendLine("IP version: ${json.optString("ipVersion", "not disclosed")}")
        appendLine("Country: ${json.optString("country", "not disclosed")}")
        appendLine("Type: ${json.optString("type", "not disclosed")}")
        appendLine("Status: ${jsonArrayStrings(json.optJSONArray("status")).joinToString(", ").ifBlank { "not disclosed" }}")
        appendLine()
        appendLine("RDAP IP data describes public allocation/registration and does not identify an individual user of the address.")
    }

    private fun normalizeHost(value: String): String? {
        val raw = value.trim().lowercase(Locale.ROOT)
        if (raw.isBlank() || raw.length > 500 || raw.any { it.isWhitespace() }) return null
        val uri = runCatching { URI(if ("://" in raw) raw else "https://$raw") }.getOrNull() ?: return null
        val host = uri.host?.trimEnd('.')?.lowercase(Locale.ROOT) ?: return null
        if (host.length > 253 || !host.contains('.')) return null
        return runCatching { IDN.toASCII(host) }.getOrNull()?.lowercase(Locale.ROOT)
    }

    private fun parseIpLiteral(value: String): InetAddress? {
        val v = value.trim()
        val looksV4 = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(v)
        val looksV6 = v.contains(':') && Regex("^[0-9A-Fa-f:.%]+$").matches(v)
        if (!looksV4 && !looksV6) return null
        return runCatching { InetAddress.getByName(v) }.getOrNull()
    }

    private fun isPrivateOrSpecial(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress || address.isMulticastAddress) return true
        val b = address.address
        if (b.size == 16) {
            val first = b[0].toInt() and 0xff
            if ((first and 0xfe) == 0xfc) return true
        }
        return false
    }

    private fun validIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
    }

    private fun authToken(value: String, name: String): String {
        return Regex("\\b${Regex.escape(name)}\\s*=\\s*([a-zA-Z0-9_-]+)").find(value)?.groupValues?.getOrNull(1) ?: "not observed"
    }

    private fun jsonArrayStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }

    private fun digest(algorithm: String, bytes: ByteArray): String = MessageDigest.getInstance(algorithm)
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun friendlyError(t: Throwable): String = t.message?.take(180) ?: t::class.java.simpleName

    private fun add(title: String, output: String, isError: Boolean = false) {
        append(AdvancedOsintResult(title, output.trim(), isError))
    }

    private fun append(result: AdvancedOsintResult) {
        _results.value = (_results.value + result).takeLast(30)
    }
}
