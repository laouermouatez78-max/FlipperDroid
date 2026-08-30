package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.ValueCard

@Composable
fun NfcScreen(viewModel: AppViewModel) {
    val snapshot by viewModel.nfc.collectAsState()
    val hardware by viewModel.hardware.collectAsState()
    val writePending by viewModel.nfcWritePending.collectAsState()
    val writeResult by viewModel.nfcWriteResult.collectAsState()
    var writeText by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "NFC Reader",
                "Approchez un tag pour lire UID, type de puce, technologies et contenu NDEF standard. Pas d'émulation de carte bancaire."
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Nfc, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                !hardware.nfcPresent -> "NFC non disponible"
                                !hardware.nfcEnabled -> "Activez le NFC dans Android"
                                writePending != null -> "En attente du tag à écrire…"
                                else -> "Lecteur actif — approchez un tag"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text("La lecture et l'écriture restent locales sur le téléphone.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (snapshot != null && writePending == null) {
                        IconButton(onClick = viewModel::clearNfc) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Effacer")
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Écrire un tag (texte NDEF)", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Écrit uniquement sur le tag que vous approchez juste après avoir appuyé sur le bouton — aucune écriture silencieuse ou automatique.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = writeText,
                        onValueChange = { writeText = it.take(500) },
                        label = { Text("Texte à écrire") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = writePending == null
                    )
                    if (writePending == null) {
                        Button(
                            onClick = { viewModel.armNfcWrite(writeText) },
                            enabled = writeText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Approcher un tag pour écrire")
                        }
                    } else {
                        OutlinedButton(onClick = viewModel::cancelNfcWrite, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Close, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Annuler l'écriture en attente")
                        }
                    }
                    writeResult?.let { result ->
                        Text(
                            result.message,
                            color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        snapshot?.let { data ->
            item { ValueCard("UID", data.uid) }
            item { ValueCard("Type de puce détecté", data.tagTypeGuess) }
            item { ValueCard("Technologies", data.technologies.joinToString().ifBlank { "Non déterminées" }) }
            item {
                val sizeLabel = data.rawSizeBytes?.let { "$it octets" } ?: "Non communiquée"
                val writableLabel = when (data.writable) {
                    true -> "oui"
                    false -> "non"
                    null -> "non déterminé"
                }
                ValueCard("Capacité NDEF / inscriptible", "$sizeLabel · inscriptible: $writableLabel")
            }
            if (data.ndefItems.isEmpty()) {
                item { ValueCard("NDEF", "Aucun enregistrement NDEF lisible") }
            } else {
                data.ndefItems.forEach { ndef ->
                    item { ValueCard(ndef.type, ndef.value) }
                }
            }
        }
    }
}
