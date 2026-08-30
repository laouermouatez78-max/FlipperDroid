package com.example.flipperdroid.view

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.net.URI
import java.util.Locale

private data class QrInspection(
    val type: String,
    val summary: String,
    val warning: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(navController: NavController) {
    var payload by remember { mutableStateOf("") }
    val inspection = remember(payload) { inspectQrPayload(payload) }
    val qrBitmap = remember(payload) {
        if (payload.isBlank()) null else runCatching { buildQrBitmap(payload, 640) }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Tools · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Local QR generator & payload inspector", fontWeight = FontWeight.Bold)
                        Text("Analyze structure before displaying the QR. Nothing is uploaded by this module.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it.take(8192) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                label = { Text("QR payload") },
                supportingText = { Text("${payload.length}/8192 characters · URL, WIFI:, mailto:, tel:, MECARD: or vCard") }
            )

            if (payload.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Payload analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Type: ${inspection.type}")
                        Text(inspection.summary, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        inspection.warning?.let {
                            AssistChip(
                                onClick = {},
                                label = { Text(it) },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                            )
                        }
                    }
                }

                qrBitmap?.let { bitmap ->
                    Card {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Generated locally", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated QR",
                                modifier = Modifier.size(260.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun inspectQrPayload(raw: String): QrInspection {
    val text = raw.trim()
    if (text.isBlank()) return QrInspection("Empty", "No payload")

    return when {
        text.startsWith("WIFI:", ignoreCase = true) -> inspectWifiQr(text)
        text.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
            val fields = text.lineSequence().map { it.substringBefore(':').uppercase(Locale.ROOT) }.filter { it.isNotBlank() }.distinct().toList()
            QrInspection("vCard", "Fields: ${fields.take(12).joinToString().ifBlank { "unknown" }}", "Review contact details before sharing")
        }
        text.startsWith("MECARD:", ignoreCase = true) -> QrInspection("MeCard", "Compact contact payload · ${text.length} characters", "May contain personal contact data")
        text.startsWith("mailto:", ignoreCase = true) -> {
            val uri = runCatching { Uri.parse(text) }.getOrNull()
            QrInspection("Email URI", "Recipient present: ${!uri?.schemeSpecificPart.isNullOrBlank()} · query present: ${uri?.query != null}", "Verify recipient and message before sending")
        }
        text.startsWith("tel:", ignoreCase = true) -> QrInspection("Telephone URI", "Telephone action payload · number hidden in analysis", "Calling can trigger an external action")
        text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true) -> inspectUrlQr(text)
        else -> QrInspection("Text", "${text.length} character(s) · ${text.toByteArray(Charsets.UTF_8).size} UTF-8 bytes")
    }
}

private fun inspectUrlQr(text: String): QrInspection {
    val uri = runCatching { URI(text) }.getOrNull()
        ?: return QrInspection("URL", "Malformed URL", "Do not open until the URL is verified")
    val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
    val host = uri.host ?: "unparsed"
    val queryNames = uri.rawQuery.orEmpty().split('&')
        .mapNotNull { it.substringBefore('=').takeIf(String::isNotBlank) }
        .distinct()
        .take(20)
    val summary = buildString {
        appendLine("Scheme: $scheme")
        appendLine("Host: $host")
        appendLine("Port: ${if (uri.port == -1) "default" else uri.port}")
        appendLine("Path length: ${uri.rawPath.orEmpty().length}")
        append("Query parameter names: ${queryNames.ifEmpty { listOf("none") }.joinToString()}")
    }
    val warning = when {
        !uri.userInfo.isNullOrBlank() -> "Embedded credentials/user-info detected"
        scheme == "http" -> "Unencrypted HTTP destination"
        host.startsWith("xn--", true) || host.contains(".xn--", true) -> "Punycode/IDN hostname — verify carefully"
        else -> "Verify the destination before opening"
    }
    return QrInspection("URL (${scheme.uppercase(Locale.ROOT)})", summary, warning)
}

private fun inspectWifiQr(text: String): QrInspection {
    val body = text.removePrefix("WIFI:").removePrefix("wifi:")
    val fields = body.split(';').mapNotNull { token ->
        val key = token.substringBefore(':', "").trim().uppercase(Locale.ROOT)
        val value = token.substringAfter(':', "")
        if (key.isBlank()) null else key to value
    }.toMap()
    val ssid = fields["S"].orEmpty()
    val security = fields["T"].orEmpty().ifBlank { "unspecified" }
    val hidden = fields["H"].equals("true", true)
    val passwordPresent = !fields["P"].isNullOrEmpty()
    val summary = buildString {
        appendLine("SSID: ${if (ssid.isBlank()) "not specified" else ssid}")
        appendLine("Security: $security")
        appendLine("Hidden network: $hidden")
        append("Password field present: $passwordPresent · value intentionally hidden")
    }
    return QrInspection("Wi‑Fi configuration", summary, if (passwordPresent) "Contains Wi‑Fi credentials — share carefully" else "Review SSID/security before connecting")
}

private fun buildQrBitmap(text: String, size: Int): Bitmap {
    val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}
