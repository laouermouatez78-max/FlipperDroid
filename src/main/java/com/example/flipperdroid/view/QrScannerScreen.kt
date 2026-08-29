package com.example.flipperdroid.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(navController: NavController) {
    var payload by remember { mutableStateOf("") }
    val classification = remember(payload) { classifyQrPayload(payload) }
    val qrBitmap = remember(payload) {
        if (payload.isBlank()) null else runCatching { buildQrBitmap(payload, 640) }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Tools") },
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
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Column {
                        Text("Local QR generator & payload inspector", fontWeight = FontWeight.Bold)
                        Text("Paste decoded QR content or create a QR locally. No data leaves the device.")
                    }
                }
            }

            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it.take(4096) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("QR payload") },
                supportingText = { Text("Text, URL, WIFI:, mailto:, tel:, MECARD: or vCard") }
            )

            if (payload.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Payload analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Type: ${classification.first}")
                        Text(classification.second, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }

                qrBitmap?.let { bitmap ->
                    Card {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated QR",
                            modifier = Modifier.size(260.dp).padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun classifyQrPayload(raw: String): Pair<String, String> {
    val text = raw.trim()
    return when {
        text.startsWith("WIFI:", ignoreCase = true) -> "Wi‑Fi configuration" to "Contains a Wi‑Fi configuration payload. Review SSID/security fields before using it."
        text.startsWith("BEGIN:VCARD", ignoreCase = true) -> "vCard" to "Contact card payload"
        text.startsWith("MECARD:", ignoreCase = true) -> "MeCard" to "Compact contact payload"
        text.startsWith("mailto:", ignoreCase = true) -> "Email" to "Email URI"
        text.startsWith("tel:", ignoreCase = true) -> "Phone" to "Telephone URI"
        text.startsWith("http://", ignoreCase = true) -> "URL (HTTP)" to "Unencrypted HTTP link — inspect destination before opening."
        text.startsWith("https://", ignoreCase = true) -> "URL (HTTPS)" to "HTTPS link — still verify the domain before opening."
        else -> "Text" to "${text.length} character(s)"
    }
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
