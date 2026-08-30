package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.components.FeatureTile
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.SectionTitle
import com.example.flipperdroid.ui.components.StatusCard

@Composable
fun HomeScreen(viewModel: AppViewModel, navController: NavController) {
    val hardware by viewModel.hardware.collectAsState()
    val network by viewModel.network.collectAsState()
    val subGhz by viewModel.subGhz.collectAsState()

    val cards = listOf(
        Triple("NFC", hardware.nfcPresent && hardware.nfcEnabled, if (hardware.nfcPresent) "Lecteur présent" else "Non disponible"),
        Triple("Bluetooth", hardware.bluetoothPresent && hardware.bluetoothEnabled, if (hardware.bluetoothPresent) "BLE disponible" else "Non disponible"),
        Triple("Wi‑Fi", hardware.wifiPresent, network.transport),
        Triple("USB Host", hardware.usbHost, if (hardware.usbHost) "OTG disponible" else "Non détecté"),
        Triple("Infrarouge", hardware.irEmitter, if (hardware.irEmitter) "Émetteur présent" else "Absent"),
        Triple("Sub-GHz", subGhz.recognizedDongle != null, if (subGhz.recognizedDongle != null) "Dongle détecté" else "Aucune radio native")
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(165.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            HeroBanner(
                title = "Cyber toolkit mobile, propre et contrôlé",
                subtitle = "Diagnostics radio et réseau pour appareils et environnements autorisés. Sans télémétrie, sans cloud, sans scan agressif automatique.",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SectionTitle("État du téléphone") {
                IconButton(onClick = viewModel::refreshAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                }
            }
        }
        items(cards) { (title, ok, detail) ->
            val icon = when (title) {
                "NFC" -> Icons.Default.Nfc
                "Bluetooth" -> Icons.Default.Bluetooth
                "Wi‑Fi" -> Icons.Default.Wifi
                "USB Host" -> Icons.Default.Usb
                "Sub-GHz" -> Icons.Default.SettingsInputAntenna
                else -> Icons.Default.SettingsRemote
            }
            StatusCard(title, detail, ok, icon)
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SectionTitle("Accès rapides")
        }
        item { FeatureTile("Réseau", "DNS, interface, passerelle et test TCP ciblé", Icons.Default.Lan, { navController.navigate("network") }) }
        item { FeatureTile("Labo BLE", "Balise contrôlée de 30 secondes pour tests de réception", Icons.Default.Science, { navController.navigate("lab") }) }
        item { FeatureTile("Sub-GHz", "État honnête + détection de dongle SDR externe", Icons.Default.SettingsInputAntenna, { navController.navigate("subghz") }) }
        item { FeatureTile("Journal", "Événements locaux, copie et partage", Icons.Default.Terminal, { navController.navigate("logs") }) }
        item { FeatureTile("Réglages", "Thème, accent et mode confidentialité", Icons.Default.Settings, { navController.navigate("settings") }) }
    }
}
