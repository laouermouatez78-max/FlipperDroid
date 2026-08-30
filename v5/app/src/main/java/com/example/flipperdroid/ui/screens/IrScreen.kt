package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
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
fun IrScreen(viewModel: AppViewModel) {
    val ir by viewModel.ir.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroBanner("Infrarouge", "Diagnostic de l'émetteur IR matériel et des bandes porteuses annoncées par Android.") }
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
        item { ValueCard("Plages porteuses", ir.carrierRanges.joinToString().ifBlank { "Aucune plage annoncée" }) }
    }
}
