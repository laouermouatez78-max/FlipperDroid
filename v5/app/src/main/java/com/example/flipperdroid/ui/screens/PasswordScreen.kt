package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.ui.components.HeroBanner
import java.security.SecureRandom

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
    }
}

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
