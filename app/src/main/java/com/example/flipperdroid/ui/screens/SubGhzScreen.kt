package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputAntenna
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
fun SubGhzScreen(viewModel: AppViewModel) {
    val status by viewModel.subGhz.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "Sub-GHz",
                "315/433/868/915 MHz — aucun smartphone Android n'embarque de radio Sub-GHz native. Cet écran l'indique honnêtement plutôt que de simuler une fonction inexistante."
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SettingsInputAntenna, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            if (status.recognizedDongle != null) "Dongle SDR/Sub-GHz détecté" else "Aucune radio Sub-GHz active",
                            fontWeight = FontWeight.Bold
                        )
                        Text(status.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        status.recognizedDongle?.let { dongle ->
            item { ValueCard("Matériel reconnu", dongle.knownDeviceLabel ?: "Dongle SDR") }
            item { ValueCard("VID:PID", "%04X:%04X".format(dongle.vendorId, dongle.productId)) }
            item { ValueCard("Nom USB", dongle.product ?: dongle.name) }
        }
        item {
            ValueCard(
                "USB Host (OTG)",
                if (status.usbHostAvailable) "Disponible — un dongle compatible peut être branché" else "Non disponible sur cet appareil"
            )
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pourquoi pas de Sub-GHz natif ?", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Les smartphones intègrent Wi‑Fi, BLE/Bluetooth, NFC et parfois IR, mais aucun chipset radio 315/433/868/915 MHz grand public. " +
                            "Un vrai travail Sub-GHz (capture, rejeu, brute-force de code fixe) nécessite un matériel externe dédié (RTL-SDR, HackRF, YARD Stick One/CC1101, Flipper Zero…) branché en USB‑OTG.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
