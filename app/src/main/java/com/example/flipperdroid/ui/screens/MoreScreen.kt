package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.ui.components.FeatureTile

@Composable
fun MoreScreen(navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text("Outils", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        }
        item { FeatureTile("Réseau", "État de liaison, DNS et TCP ciblé", Icons.Default.Lan, { navController.navigate("network") }) }
        item { FeatureTile("USB", "Inventaire VID/PID, interfaces et endpoints", Icons.Default.Usb, { navController.navigate("usb") }) }
        item { FeatureTile("Infrarouge", "Capacité matérielle, bandes porteuses et test d'émission borné", Icons.Default.SettingsRemote, { navController.navigate("ir") }) }
        item { FeatureTile("Sub-GHz", "État honnête + détection de dongle SDR externe", Icons.Default.SettingsInputAntenna, { navController.navigate("subghz") }) }
        item { FeatureTile("Mot de passe", "Générateur local avec SecureRandom", Icons.Default.VpnKey, { navController.navigate("password") }) }
        item { FeatureTile("Labo BLE", "Émission d'une balise fixe et limitée", Icons.Default.Science, { navController.navigate("lab") }) }
        item { FeatureTile("Logs", "Journal local exportable", Icons.Default.Terminal, { navController.navigate("logs") }) }
        item { FeatureTile("Réglages", "Thème et confidentialité", Icons.Default.Settings, { navController.navigate("settings") }) }
        item { FeatureTile("À propos", "Version, philosophie et limites", Icons.Default.Info, { navController.navigate("about") }) }
    }
}
