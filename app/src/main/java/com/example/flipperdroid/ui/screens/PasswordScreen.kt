package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.ui.components.HeroBanner
import java.security.SecureRandom
import kotlin.math.log2

@Composable
fun PasswordScreen() {
    var length by remember { mutableFloatStateOf(24f) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    fun generate() {
        password = securePassword(length.toInt(), upper, lower, digits, symbols)
    }

    LaunchedEffect(Unit) { generate() }

    val poolSize = remember(upper, lower, digits, symbols) {
        var size = 0
        if (lower) size += 25 // matches the exact charset below (ambiguous chars excluded)
        if (upper) size += 24
        if (digits) size += 8
        if (symbols) size += 12
        size
    }
    val entropyBits = remember(password, poolSize) {
        if (password.isBlank() || poolSize <= 1) 0.0 else password.length * log2(poolSize.toDouble())
    }
    val strength = remember(entropyBits) {
        when {
            entropyBits >= 100 -> PasswordStrength("Très robuste", Color(0xFF2ECC71))
            entropyBits >= 70 -> PasswordStrength("Robuste", Color(0xFF52D0A0))
            entropyBits >= 45 -> PasswordStrength("Correcte", Color(0xFFF5B041))
            entropyBits > 0 -> PasswordStrength("Faible", Color(0xFFE74C3C))
            else -> PasswordStrength("—", Color.Gray)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeroBanner("Password Lab", "Générateur entièrement local basé sur SecureRandom. Aucun mot de passe n'est envoyé sur Internet.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Longueur: ${length.toInt()}", fontWeight = FontWeight.Bold)
                    Slider(value = length, onValueChange = { length = it }, valueRange = 8f..64f, steps = 55)
                    ToggleRow("Minuscules", lower) { lower = it }
                    ToggleRow("Majuscules", upper) { upper = it }
                    ToggleRow("Chiffres", digits) { digits = it }
                    ToggleRow("Symboles", symbols) { symbols = it }
                    HorizontalDivider()
                    Text(password, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Force: ${strength.label}",
                            color = strength.color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "≈ ${entropyBits.toInt()} bits d'entropie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (entropyBits / 128.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = strength.color
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::generate, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Générer")
                        }
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(password)) },
                            enabled = password.isNotBlank()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "L'entropie est calculée sur la taille réelle du jeu de caractères sélectionné (log2(jeu) × longueur), à titre indicatif — elle suppose un tirage vraiment aléatoire, ce que SecureRandom garantit ici.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class PasswordStrength(val label: String, val color: Color)

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun securePassword(length: Int, upper: Boolean, lower: Boolean, digits: Boolean, symbols: Boolean): String {
    val groups = buildList {
        if (lower) add("abcdefghijkmnopqrstuvwxyz")
        if (upper) add("ABCDEFGHJKLMNPQRSTUVWXYZ")
        if (digits) add("23456789")
        if (symbols) add("!@#%&*+-_=?.")
    }
    if (groups.isEmpty()) return "Sélectionnez au moins un jeu de caractères"
    val random = SecureRandom()
    val all = groups.joinToString("")
    val chars = MutableList(length.coerceAtLeast(groups.size)) { all[random.nextInt(all.length)] }
    groups.forEachIndexed { index, group -> chars[index] = group[random.nextInt(group.length)] }
    for (i in chars.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = chars[i]
        chars[i] = chars[j]
        chars[j] = tmp
    }
    return chars.joinToString("")
}
