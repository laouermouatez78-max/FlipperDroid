package com.example.flipperdroid.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale


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
        val output = buildString {
            appendLine("Display name: ${displayName.ifBlank { "—" }}")
            appendLine("Handle: ${alias.ifBlank { "—" }}")
            appendLine("Normalized name: ${listOf(normalizedFirst, normalizedLast).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "—" }}")
            appendLine("Candidate handle variants: ${variants.take(12).joinToString(", ").ifBlank { "—" }}")
            appendLine()
            appendLine("Public-search query pack:")
            publicQueries.take(8).forEach { appendLine("• $it") }
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
            links = links.distinctBy { it.url }.take(15)
        )
    }

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
            appendLine("Internationalized form: ${runCatching { IDN.toASCII(host) }.getOrDefault(host)}")
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

    private fun slug(value: String): String {
        if (value.isBlank()) return ""
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
            .take(40)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun addResult(
        title: String,
        output: String,
        isError: Boolean = false,
        links: List<OsintLink> = emptyList()
    ) {
        _results.value = (_results.value + OsintResult(title, output.trim(), isError, links)).takeLast(20)
    }

    private fun looksLikeIp(value: String): Boolean =
        Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$").matches(value) || value.contains(':')
}
