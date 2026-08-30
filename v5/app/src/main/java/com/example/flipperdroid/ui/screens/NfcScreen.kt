package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "NFC Reader",
                "Approchez un tag pour lire UID, technologies et contenu NDEF standard. Pas d'émulation de carte bancaire ni d'écriture automatique."
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
                                else -> "Lecteur actif — approchez un tag"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text("La lecture reste locale sur le téléphone.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (snapshot != null) {
                        IconButton(onClick = viewModel::clearNfc) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Effacer")
                        }
                    }
                }
            }
        }
        snapshot?.let { data ->
            item { ValueCard("UID", data.uid) }
            item { ValueCard("Technologies", data.technologies.joinToString().ifBlank { "Non déterminées" }) }
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
