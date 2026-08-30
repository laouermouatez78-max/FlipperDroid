package com.example.flipperdroid.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.WifiNetwork
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.SignalPill
import com.example.flipperdroid.ui.components.maskMac

@Composable
fun WifiScreen(viewModel: AppViewModel) {
    val networks by viewModel.wifiNetworks.collectAsState()
    val status by viewModel.wifiStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var filter by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        viewModel.scanWifi()
    }

    fun requestAndScan() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toTypedArray()
        launcher.launch(permissions)
    }

    val visible = remember(networks, filter) {
        if (filter.isBlank()) networks else networks.filter {
            it.ssid.contains(filter, true) || it.bssid.contains(filter, true) || it.security.contains(filter, true)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "Wi‑Fi Analyzer",
                "SSID, BSSID, canal, bande, sécurité et RSSI. Android peut limiter la fréquence des scans; les résultats en cache restent affichés."
            )
        }
        item {
            Button(onClick = ::requestAndScan, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Actualiser les réseaux")
            }
        }
        item {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it.take(80) },
                label = { Text("Filtrer SSID / BSSID / sécurité") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(visible, key = { it.bssid }) { network ->
            WifiCard(network, settings.privacyMode)
        }
    }
}

@Composable
private fun WifiCard(network: WifiNetwork, privacyMode: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(network.ssid, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SignalPill(network.rssi)
            }
            Text(maskMac(network.bssid, privacyMode), style = MaterialTheme.typography.bodySmall)
            val band = when {
                network.frequencyMhz < 3000 -> "2,4 GHz"
                network.frequencyMhz < 5955 -> "5 GHz"
                network.frequencyMhz < 10_000 -> "6 GHz"
                else -> "60 GHz"
            }
            Text(
                "$band · ${network.frequencyMhz} MHz · canal ${network.channel ?: "?"} · ${network.security}",
                style = MaterialTheme.typography.bodySmall
            )
            if (network.security == "Ouvert" || network.security == "WEP") {
                Text(
                    "Sécurité faible",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
