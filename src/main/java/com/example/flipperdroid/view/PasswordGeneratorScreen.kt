package com.example.flipperdroid.view

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.SecureRandom
import kotlin.math.log2

private val passwordSecureRandom = SecureRandom()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(navController: NavController) {
    val context = LocalContext.current
    var length by remember { mutableIntStateOf(20) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var revealPassword by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    val selectedSets = remember(useUpper, useLower, useDigits, useSymbols) {
        buildList {
            if (useUpper) add("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (useLower) add("abcdefghijklmnopqrstuvwxyz")
            if (useDigits) add("0123456789")
            if (useSymbols) add("!@#${'$'}%^&*()-_=+[]{};:,.<>?/|")
        }
    }
    val poolSize = selectedSets.sumOf { it.length }
    val entropyBits = if (poolSize > 0) length * log2(poolSize.toDouble()) else 0.0
    val strength = when {
        entropyBits >= 120 -> "Very strong"
        entropyBits >= 80 -> "Strong"
        entropyBits >= 60 -> "Good"
        else -> "Limited"
    }

    fun generatePassword() {
        if (selectedSets.isEmpty() || length < selectedSets.size) {
            password = ""
            copied = false
            showQr = false
            return
        }
        password = securePassword(length, selectedSets)
        copied = false
        showQr = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator · V5") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Cryptographic local generator", style = MaterialTheme.typography.titleMedium)
                    Text("Uses java.security.SecureRandom and guarantees at least one character from every selected category.", style = MaterialTheme.typography.bodySmall)
                    Text("Estimated entropy: %.0f bits · %s".format(entropyBits, strength), color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Length: $length", modifier = Modifier.weight(1f))
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.toInt().coerceIn(8, 96) },
                    valueRange = 8f..96f,
                    steps = 87,
                    modifier = Modifier.weight(2f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useUpper, onCheckedChange = { useUpper = it })
                Text("Uppercase", modifier = Modifier.weight(1f))
                Checkbox(checked = useLower, onCheckedChange = { useLower = it })
                Text("Lowercase", modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useDigits, onCheckedChange = { useDigits = it })
                Text("Digits", modifier = Modifier.weight(1f))
                Checkbox(checked = useSymbols, onCheckedChange = { useSymbols = it })
                Text("Symbols", modifier = Modifier.weight(1f))
            }

            Button(
                onClick = ::generatePassword,
                enabled = selectedSets.isNotEmpty() && length >= selectedSets.size,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (password.isBlank()) "Generate secure password" else "Generate another")
            }

            if (selectedSets.isEmpty()) {
                Text("Select at least one character category.", color = MaterialTheme.colorScheme.error)
            }

            if (password.isNotEmpty()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {},
                    label = { Text("Generated password") },
                    readOnly = true,
                    visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { revealPassword = !revealPassword }) {
                            Icon(
                                if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (revealPassword) "Hide password" else "Reveal password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            copySensitiveText(context, password)
                            copied = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (copied) "Copied" else "Copy")
                    }
                    OutlinedButton(
                        onClick = { showQr = !showQr },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (showQr) "Hide QR" else "Show QR")
                    }
                }

                Text(
                    "Clipboard contents and QR codes can expose passwords to other people or apps. The QR stays hidden until you explicitly request it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showQr) {
                    val qrBitmap = remember(password) { generateQrCodeBitmap(password) }
                    if (qrBitmap != null) {
                        Card {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sensitive password QR", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(8.dp))
                                Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "Password QR code", modifier = Modifier.size(220.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun securePassword(length: Int, sets: List<String>): String {
    val pool = sets.joinToString("")
    val chars = MutableList(length) { pool[passwordSecureRandom.nextInt(pool.length)] }
    sets.forEachIndexed { index, set -> chars[index] = set[passwordSecureRandom.nextInt(set.length)] }
    for (i in chars.lastIndex downTo 1) {
        val j = passwordSecureRandom.nextInt(i + 1)
        val temp = chars[i]
        chars[i] = chars[j]
        chars[j] = temp
    }
    return chars.joinToString("")
}

private fun copySensitiveText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("FlipperDroid password", text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)
}

fun generateQrCodeBitmap(text: String, size: Int = 512): Bitmap? = runCatching {
    val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}.getOrNull()
