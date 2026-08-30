package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.ValueCard

@Composable
fun IrScreen(viewModel: AppViewModel) {
    val ir by viewModel.ir.collectAsState()
    val transmitResult by viewModel.irTransmitResult.collectAsState()
    var frequencyText by remember { mutableStateOf("38000") }
    var advancedMode by remember { mutableStateOf(false) }
    var patternText by remember { mutableStateOf("2000,1000,2000,1000,2000") }

    val customPattern = remember(patternText) {
        patternText.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroBanner("Infrarouge", "Diagnostic de l'émetteur IR matériel et test d'émission borné, sans base de codes télécommandes préchargée.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SettingsRemote, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(if (ir.hasEmitter) "Émetteur IR disponible" else "Aucun émetteur IR", fontWeight = FontWeight.Bold)
                        Text("Cette version n'envoie pas automatiquement de commandes à des appareils tiers.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { ValueCard("Plages porteuses annoncées", ir.carrierRanges.joinToString().ifBlank { "Aucune plage annoncée" }) }
        if (ir.hasEmitter) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Test d'émission (labo)", fontWeight = FontWeight.Bold)
                        Text(
                            "Émet une impulsion courte et bornée (max 0,5 s, max 32 segments) à la fréquence saisie, pour vérifier physiquement que le matériel émet. Aucun code de télécommande tiers n'est stocké dans l'application.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = frequencyText,
                            onValueChange = { frequencyText = it.filter(Char::isDigit).take(6) },
                            label = { Text("Fréquence porteuse (Hz)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Motif personnalisé (avancé)", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = advancedMode, onCheckedChange = { advancedMode = it })
                        }
                        if (advancedMode) {
                            OutlinedTextField(
                                value = patternText,
                                onValueChange = { patternText = it.filter { c -> c.isDigit() || c == ',' }.take(200) },
                                label = { Text("Durées on/off en µs, séparées par des virgules") },
                                supportingText = { Text("Ex: 2000,1000,2000 — max 32 segments, 0,5 s au total. Commence toujours par une impulsion \"on\".") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = {
                                val freq = frequencyText.toIntOrNull() ?: return@Button
                                val pattern = if (advancedMode) customPattern.toIntArray() else intArrayOf(2000, 1000, 2000, 1000, 2000)
                                viewModel.transmitIrTest(freq, pattern)
                            },
                            enabled = frequencyText.toIntOrNull() != null && (!advancedMode || customPattern.isNotEmpty()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (advancedMode) "Émettre le motif personnalisé" else "Émettre l'impulsion de test")
                        }
                        transmitResult?.let {
                            Text(
                                it.message,
                                color = if (it.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
