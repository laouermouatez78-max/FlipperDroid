package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import java.security.cert.X509Certificate


data class OsintLink(
    val label: String,
    val url: String
)

data class OsintResult(
    val title: String,
    val output: String,
    val isError: Boolean = false,
    val links: List<OsintLink> = emptyList()
)

class OsintViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<OsintResult>>(emptyList())
    val results: StateFlow<List<OsintResult>> = _results

    fun inspectIdentity(firstName: String, lastName: String, handle: String, publicEmail: String) {
        val first = firstName.trim().take(80)
        val last = lastName.trim().take(80)
        val alias = handle.trim().removePrefix("@").take(64)
        val email = publicEmail.trim().take(254)

        if (first.isBlank() && last.isBlank() && alias.isBlank() && email.isBlank()) {
            addResult("Public Identity Inspector", "Enter a first name, last name, public handle or public email.", true)
            return
        }

        if (alias.isNotBlank() && !Regex("^[\\p{L}\\p{N}._-]{1,64}$").matches(alias)) {
            addResult("Public Identity Inspector", "The handle may contain letters, numbers, dots, underscores and hyphens.", true)
            return
        }

        val displayName = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
        val normalizedFirst = slug(first)
        val normalizedLast = slug(last)
        val variants = linkedSetOf<String>()

        if (normalizedFirst.isNotBlank() && normalizedLast.isNotBlank()) {
            variants += normalizedFirst + normalizedLast
            variants += "$normalizedFirst.$normalizedLast"
            variants += "${normalizedFirst}_$normalizedLast"
            variants += "${normalizedFirst}-$normalizedLast"
            variants += normalizedFirst.first() + normalizedLast
            variants += normalizedFirst + normalizedLast.first()
        }
        if (alias.isNotBlank()) {
            variants += alias.lowercase(Locale.ROOT)
            variants += alias.replace("_", ".")
            variants += alias.replace(".", "_")
            variants += alias.replace("-", "_")
        }

        val links = mutableListOf<OsintLink>()
        if (alias.isNotBlank()) {
            val safe = alias.lowercase(Locale.ROOT)
            links += OsintLink("GitHub", "https://github.com/$safe")
            links += OsintLink("Reddit", "https://www.reddit.com/user/$safe/")
            links += OsintLink("X", "https://x.com/$safe")
            links += OsintLink("Instagram", "https://www.instagram.com/$safe/")
            links += OsintLink("TikTok", "https://www.tiktok.com/@$safe")
            links += OsintLink("YouTube", "https://www.youtube.com/@$safe")
            links += OsintLink("Twitch", "https://www.twitch.tv/$safe")
            links += OsintLink("Medium", "https://medium.com/@$safe")
            if (safe.contains('.')) links += OsintLink("Bluesky", "https://bsky.app/profile/$safe")
        }

        val publicQueries = linkedSetOf<String>()
        if (displayName.isNotBlank()) publicQueries += "\"$displayName\""
        if (alias.isNotBlank()) publicQueries += "\"$alias\""
        if (displayName.isNotBlank() && alias.isNotBlank()) publicQueries += "\"$displayName\" \"$alias\""
        if (alias.isNotBlank()) {
            publicQueries += "site:github.com \"$alias\""
            publicQueries += "site:reddit.com \"$alias\""
            publicQueries += "site:youtube.com \"$alias\""
            publicQueries += "site:medium.com \"$alias\""
        }

        val searchSeed = when {
            displayName.isNotBlank() && alias.isNotBlank() -> "\"$displayName\" \"$alias\""
            alias.isNotBlank() -> "\"$alias\""
            else -> "\"$displayName\""
        }
        if (searchSeed.isNotBlank()) {
            val encoded = encode(searchSeed)
            links += OsintLink("Google public search", "https://www.google.com/search?q=$encoded")
            links += OsintLink("DuckDuckGo public search", "https://duckduckgo.com/?q=$encoded")
            links += OsintLink("Bing public search", "https://www.bing.com/search?q=$encoded")
        }

        val emailInfo = inspectPublicEmailLocally(email)
        if (email.contains('@')) {
            val domain = email.substringAfterLast('@').lowercase(Locale.ROOT)
            if (isHostnameLike(domain)) {
                links += OsintLink("Email domain RDAP", "https://rdap.org/domain/${encodePath(domain)}")
            }
        }

        val output = buildString {
            appendLine("Display name: ${displayName.ifBlank { "—" }}")
            appendLine("Handle: ${alias.ifBlank { "—" }}")
            appendLine("Normalized name: ${listOf(normalizedFirst, normalizedLast).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "—" }}")
            appendLine("Candidate handle variants: ${variants.take(12).joinToString(", ").ifBlank { "—" }}")
            appendLine()
            appendLine("Public-search query pack:")
            publicQueries.take(10).forEach { appendLine("• $it") }
            if (publicQueries.isEmpty()) appendLine("• —")
            if (emailInfo.isNotBlank()) {
                appendLine()
                append(emailInfo)
            }
            appendLine()
            appendLine("Candidate profile links do not prove that an account exists or belongs to the same person.")
            appendLine("This module does not query private accounts, breach databases, home addresses or phone-owner data.")
        }

        addResult(
            title = "Public Identity · ${displayName.ifBlank { alias.ifBlank { "identifier" } }}",
            output = output,
            links = links.distinctBy { it.url }.take(18)
        )
    }

    fun inspectDomain(value: String) {
        val host = extractHost(value)
        if (host == null) {
            addResult("Domain Inspector", "Enter a valid public hostname or domain.", true)
            return
        }

        val labels = host.split('.').filter { it.isNotBlank() }
        val tld = labels.lastOrNull().orEmpty()
        val registrableHint = if (labels.size >= 2) labels.takeLast(2).joinToString(".") else host
        val ascii = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        val links = domainPassiveLinks(host)

        val output = buildString {
            appendLine("Host: $host")
            appendLine("Labels: ${labels.size}")
            appendLine("TLD / suffix hint: .$tld")
            appendLine("Registrable-domain hint: $registrableHint")
            appendLine("ASCII / IDN form: $ascii")
            appendLine("Punycode present: ${ascii.contains("xn--")}")
            appendLine()
            appendLine("Use Live DNS / Web Surface below for network-backed public checks.")
        }
        addResult("Domain Inspector · $host", output, links = links)
    }

    fun resolveDnsLive(value: String) {
        val host = extractHost(value)
        if (host == null) {
            addResult("Live DNS", "Enter a valid hostname or URL.", true)
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val addresses = InetAddress.getAllByName(host).distinctBy { it.hostAddress }
                    if (addresses.isEmpty()) error("No A/AAAA resolution returned")
                    val output = buildString {
                        appendLine("Host: $host")
                        appendLine("Resolved addresses: ${addresses.size}")
                        addresses.take(12).forEachIndexed { index, address ->
                            appendLine("${index + 1}. ${address.hostAddress} · ${addressScope(address)}")
                            val canonical = runCatching { address.canonicalHostName }.getOrNull()
                            if (!canonical.isNullOrBlank() && canonical != address.hostAddress && canonical != host) {
                                appendLine("   reverse/canonical: $canonical")
                            }
                        }
                    }
                    OsintResult("Live DNS · $host", output.trim(), links = domainPassiveLinks(host))
                }.getOrElse { error ->
                    OsintResult("Live DNS · $host", "DNS lookup failed: ${friendlyError(error)}", true)
                }
            }
            appendResult(result)
        }
    }

    fun inspectWebSurface(value: String) {
        val uri = parseHttpUri(value)
        if (uri == null) {
            addResult("Public Web Surface", "Enter a valid HTTP or HTTPS URL.", true)
            return
        }
        if (!uri.userInfo.isNullOrBlank()) {
            addResult("Public Web Surface", "URLs containing embedded user-info or credentials are not accepted.", true)
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val host = uri.host.lowercase(Locale.ROOT)
                    val resolved = InetAddress.getAllByName(host).distinctBy { it.hostAddress }
                    if (resolved.isEmpty()) error("Host did not resolve")
                    if (resolved.all { isPrivateOrSpecial(it) }) {
                        return@runCatching OsintResult(
                            "Public Web Surface · $host",
                            "The target resolves only to local/private/special-use addresses. Active web inspection is limited to public OSINT targets.",
                            true
                        )
                    }

                    val url = URL(uri.toASCIIString())
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "HEAD"
                        connectTimeout = 6000
                        readTimeout = 6000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "FlipperDroid-V5-OSINT/5.0")
                        setRequestProperty("Accept", "*/*")
                    }

                    try {
                        val status = connection.responseCode
                        val location = connection.getHeaderField("Location")
                        val server = connection.getHeaderField("Server")
                        val contentType = connection.getHeaderField("Content-Type")
                        val securityHeaders = listOf(
                            "Strict-Transport-Security",
                            "Content-Security-Policy",
                            "X-Content-Type-Options",
                            "Referrer-Policy",
                            "Permissions-Policy",
                            "Cross-Origin-Opener-Policy",
                            "Cross-Origin-Resource-Policy"
                        )
                        val present = securityHeaders.filter { !connection.getHeaderField(it).isNullOrBlank() }
                        val missing = securityHeaders - present.toSet()

                        val tlsSummary = if (connection is HttpsURLConnection) {
                            buildTlsSummary(connection)
                        } else {
                            "TLS: not used (HTTP)"
                        }

                        val output = buildString {
                            appendLine("URL: ${uri.toASCIIString()}")
                            appendLine("HTTP status: $status")
                            appendLine("Resolved: ${resolved.take(6).joinToString(", ") { it.hostAddress ?: "?" }}")
                            appendLine("Server header: ${server ?: "not disclosed"}")
                            appendLine("Content-Type: ${contentType ?: "not disclosed"}")
                            appendLine("Redirect location: ${location ?: "none"}")
                            appendLine()
                            appendLine("Security headers present (${present.size}/${securityHeaders.size}):")
                            if (present.isEmpty()) appendLine("• none observed in HEAD response") else present.forEach { appendLine("• $it") }
                            if (missing.isNotEmpty()) {
                                appendLine("Missing/not observed:")
                                missing.forEach { appendLine("• $it") }
                            }
                            appendLine()
                            append(tlsSummary)
                        }

                        OsintResult(
                            "Public Web Surface · $host",
                            output.trim(),
                            links = webDiscoveryLinks(uri)
                        )
                    } finally {
                        connection.disconnect()
                    }
                }.getOrElse { error ->
                    OsintResult(
                        "Public Web Surface · ${uri.host}",
                        "Web/TLS inspection failed: ${friendlyError(error)}",
                        true,
                        links = webDiscoveryLinks(uri)
                    )
                }
            }
            appendResult(result)
        }
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

        val output = buildString {
            appendLine("Address: ${address.hostAddress}")
            appendLine("Family: ${if (address.address.size == 16) "IPv6" else "IPv4"}")
            appendLine("Scope: ${addressScope(address)}")
            appendLine("Loopback: ${address.isLoopbackAddress}")
            appendLine("Link-local: ${address.isLinkLocalAddress}")
            appendLine("Private/site-local: ${address.isSiteLocalAddress || isUniqueLocalIpv6(address)}")
            appendLine("Multicast: ${address.isMulticastAddress}")
            val reverse = runCatching { address.canonicalHostName }.getOrNull()
            if (!reverse.isNullOrBlank() && reverse != address.hostAddress) appendLine("Reverse/canonical: $reverse")
        }
        addResult("IP Inspector · ${address.hostAddress}", output)
    }

    fun inspectUrl(value: String) {
        val uri = parseHttpUri(value)
        if (uri == null) {
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
            if (!uri.rawQuery.isNullOrBlank()) {
                appendLine("Query parameter names: ${queryParameterNames(uri.rawQuery).joinToString(", ").ifBlank { "—" }}")
            }
        }
        addResult("URL Inspector · ${uri.host}", output, links = webDiscoveryLinks(uri))
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    private fun buildTlsSummary(connection: HttpsURLConnection): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cert = runCatching {
            connection.serverCertificates.firstOrNull() as? X509Certificate
        }.getOrNull()

        if (cert == null) return "TLS: HTTPS connection established, certificate details unavailable."

        val sanDns = runCatching {
            cert.subjectAlternativeNames
                ?.mapNotNull { entry ->
                    val type = entry.getOrNull(0) as? Int
                    val value = entry.getOrNull(1)?.toString()
                    if (type == 2 && !value.isNullOrBlank()) value else null
                }
                ?.distinct()
                ?.take(12)
                .orEmpty()
        }.getOrDefault(emptyList())

        return buildString {
            appendLine("TLS certificate:")
            appendLine("• subject: ${cert.subjectX500Principal.name}")
            appendLine("• issuer: ${cert.issuerX500Principal.name}")
            appendLine("• valid from: ${dateFormat.format(cert.notBefore)}")
            appendLine("• valid until: ${dateFormat.format(cert.notAfter)}")
            appendLine("• serial: ${cert.serialNumber.toString(16)}")
            appendLine("• signature algorithm: ${cert.sigAlgName}")
            if (sanDns.isNotEmpty()) appendLine("• DNS SANs: ${sanDns.joinToString(", ")}")
        }
    }

    private fun domainPassiveLinks(host: String): List<OsintLink> {
        val encodedHost = encode(host)
        return listOf(
            OsintLink("RDAP registration data", "https://rdap.org/domain/${encodePath(host)}"),
            OsintLink("Certificate Transparency", "https://crt.sh/?q=%25.$encodedHost"),
            OsintLink("Wayback Machine", "https://web.archive.org/web/*/https://$host/*"),
            OsintLink("urlscan.io public results", "https://urlscan.io/domain/$host"),
            OsintLink("Google site index", "https://www.google.com/search?q=${encode("site:$host")}")
        )
    }

    private fun webDiscoveryLinks(uri: URI): List<OsintLink> {
        val host = uri.host.lowercase(Locale.ROOT)
        val origin = "${uri.scheme}://$host${if (uri.port == -1) "" else ":${uri.port}"}"
        return (domainPassiveLinks(host) + listOf(
            OsintLink("security.txt", "$origin/.well-known/security.txt"),
            OsintLink("robots.txt", "$origin/robots.txt"),
            OsintLink("sitemap.xml", "$origin/sitemap.xml")
        )).distinctBy { it.url }
    }

    private fun inspectPublicEmailLocally(value: String): String {
        if (value.isBlank()) return ""
        val at = value.lastIndexOf('@')
        if (at <= 0 || at == value.lastIndex || value.any { it.isWhitespace() }) {
            return "Public email: invalid format (local parsing only).\n"
        }
        val local = value.substring(0, at)
        val domain = value.substring(at + 1).lowercase(Locale.ROOT)
        val asciiDomain = runCatching { IDN.toASCII(domain) }.getOrDefault(domain)
        return buildString {
            appendLine("Public email metadata (local only):")
            appendLine("• local-part length: ${local.length}")
            appendLine("• domain: $domain")
            appendLine("• ASCII domain: $asciiDomain")
            appendLine("• plus-addressing present: ${local.contains('+')}")
        }
    }

    private fun extractHost(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank() || raw.length > 500 || raw.any { it == '\n' || it == '\r' }) return null
        val host = runCatching {
            URI(if ("://" in raw) raw else "https://$raw").host
        }.getOrNull()?.trimEnd('.')?.lowercase(Locale.ROOT) ?: return null
        if (!isHostnameLike(host)) return null
        return host
    }

    private fun parseHttpUri(value: String): URI? {
        val raw = value.trim()
        if (raw.isBlank() || raw.length > 1000 || raw.any { it == '\n' || it == '\r' }) return null
        val uri = runCatching { URI(if ("://" in raw) raw else "https://$raw") }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        return uri
    }

    private fun isHostnameLike(host: String): Boolean {
        if (host.length !in 1..253 || host.any { it.isWhitespace() }) return false
        val ascii = runCatching { IDN.toASCII(host) }.getOrNull() ?: return false
        return ascii.split('.').all { label ->
            label.isNotBlank() && label.length <= 63 &&
                Regex("^[A-Za-z0-9-]+$").matches(label) &&
                !label.startsWith('-') && !label.endsWith('-')
        }
    }

    private fun addressScope(address: InetAddress): String = when {
        address.isLoopbackAddress -> "loopback"
        address.isLinkLocalAddress -> "link-local"
        address.isSiteLocalAddress || isUniqueLocalIpv6(address) -> "private/site-local"
        address.isMulticastAddress -> "multicast"
        address.isAnyLocalAddress -> "unspecified/local"
        else -> "public/global or provider-routed"
    }

    private fun isPrivateOrSpecial(address: InetAddress): Boolean =
        address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress ||
            address.isAnyLocalAddress ||
            isUniqueLocalIpv6(address)

    private fun isUniqueLocalIpv6(address: InetAddress): Boolean {
        val bytes = address.address
        return bytes.size == 16 && ((bytes[0].toInt() and 0xFE) == 0xFC)
    }

    private fun queryParameterNames(rawQuery: String): List<String> =
        rawQuery.split('&').mapNotNull { pair ->
            pair.substringBefore('=').takeIf { it.isNotBlank() }
        }.distinct().take(20)

    private fun slug(value: String): String {
        if (value.isBlank()) return ""
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
            .take(40)
    }

    private fun friendlyError(error: Throwable): String =
        error.message?.take(180)?.ifBlank { error.javaClass.simpleName } ?: error.javaClass.simpleName

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun encodePath(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun addResult(
        title: String,
        output: String,
        isError: Boolean = false,
        links: List<OsintLink> = emptyList()
    ) {
        appendResult(OsintResult(title, output.trim(), isError, links))
    }

    private fun appendResult(result: OsintResult) {
        _results.value = (_results.value + result).takeLast(24)
    }

    private fun looksLikeIp(value: String): Boolean =
        Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(value) || value.contains(':')
}
