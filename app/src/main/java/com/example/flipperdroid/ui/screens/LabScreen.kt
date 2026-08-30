package com.example.flipperdroid.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.services.BleLabBeacon
import com.example.flipperdroid.ui.components.HeroBanner

@Composable
fun LabScreen(viewModel: AppViewModel) {
    val state by viewModel.beaconState.collectAsState()
    val hardware by viewModel.hardware.collectAsState()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        viewModel.startLabBeacon()
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            viewModel.startLabBeacon()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroBanner(
                "BLE Lab",
                "Démonstrateur actif volontairement limité: une seule balise, faible puissance, non connectable, UUID fixe, arrêt Android après 30 secondes."
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Science, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Balise de vérification", fontWeight = FontWeight.Bold)
                    }
                    Text("UUID", style = MaterialTheme.typography.labelMedium)
                    Text(BleLabBeacon.LAB_UUID, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (hardware.bleAdvertisingSupported) "Publicité BLE supportée" else "Support BLE actif non confirmé (permission ou matériel)",
                        color = if (hardware.bleAdvertisingSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::start, enabled = !state.active, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Émettre 30 s")
                        }
                        OutlinedButton(onClick = viewModel::stopLabBeacon, enabled = state.active) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "Utilisez ce mode uniquement autour de votre propre matériel ou dans une zone de test autorisée. Il ne contient ni flooding, ni usurpation de marque, ni brouillage radio.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
