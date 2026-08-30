package com.example.flipperdroid.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.WifiNetwork
import com.example.flipperdroid.ui.components.ChannelOccupancyChart
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.SignalPill
import com.example.flipperdroid.ui.components.maskMac

@Composable
fun WifiScreen(viewModel: AppViewModel) {
    val networks by viewModel.wifiNetworks.collectAsState()
    val status by viewModel.wifiStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var filter by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        // scanWifi() reports the exact missing prerequisite (precise location, Wi‑Fi, or Location Services).
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

    fun shareCsv() {
        val csv = viewModel.exportWifiCsv()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid — export scan Wi‑Fi")
        }
        context.startActivity(Intent.createChooser(intent, "Partager l'export Wi‑Fi"))
    }

    val visible = remember(networks, filter) {
        if (filter.isBlank()) networks else networks.filter {
            it.ssid.contains(filter, true) || it.bssid.contains(filter, true) || it.security.contains(filter, true)
        }
    }

    // 2.4 GHz channels 1/6/11 are the only non-overlapping ones in most regions;
    // flag networks sitting on a crowded/overlapping channel to help pick a clean one.
    val crowded2GhzChannels = remember(visible) {
        visible.filter { it.band == "2,4 GHz" }
            .mapNotNull { it.channel }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
    }

    val channel24Counts = remember(networks) {
        networks.filter { it.band == "2,4 GHz" }
            .mapNotNull { it.channel }
            .groupingBy { it }
            .eachCount()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "Wi‑Fi Analyzer",
                "SSID, BSSID, canal, largeur, standard, sécurité et RSSI. Android peut limiter la fréquence des scans; les résultats en cache restent affichés."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = ::requestAndScan, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Actualiser les réseaux")
                }
                IconButton(onClick = ::shareCsv, enabled = networks.isNotEmpty()) {
                    Icon(Icons.Default.IosShare, contentDescription = "Exporter CSV")
                }
            }
        }
        item {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (channel24Counts.isNotEmpty()) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Occupation des canaux 2,4 GHz", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        ChannelOccupancyChart(channel24Counts)
                        Text(
                            "Canaux 1/6/11 en surbrillance — les seuls à ne pas se chevaucher dans la plupart des régions.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
            WifiCard(network, settings.privacyMode, crowded = network.channel in crowded2GhzChannels)
        }
    }
}

@Composable
private fun WifiCard(network: WifiNetwork, privacyMode: Boolean, crowded: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(network.ssid, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                SignalPill(network.rssi)
            }
            Text(maskMac(network.bssid, privacyMode), style = MaterialTheme.typography.bodySmall)
            Text(
                "${network.band} · ${network.frequencyMhz} MHz · canal ${network.channel ?: "?"} · ${network.channelWidth} · ${network.security}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(network.standard, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (network.security == "Ouvert" || network.security == "WEP") {
                    Text(
                        "Sécurité faible",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (crowded) {
                    Text(
                        "Canal 2,4 GHz partagé",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
