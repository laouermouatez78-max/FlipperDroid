package com.example.flipperdroid.view

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.BleAdvertiserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleAdvertiserScreen(navController: NavController, viewModel: BleAdvertiserViewModel) {
    val active by viewModel.isAdvertising.collectAsState()
    val status by viewModel.status.collectAsState()
    val name by viewModel.localName.collectAsState()

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else emptyArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Advertiser · V4") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Real BLE test beacon", fontWeight = FontWeight.Bold)
                        Text("Advertises a FlipperDroid V4 test UUID so another device you control can detect this phone.")
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = viewModel::updateName,
                label = { Text("BLE device name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !active
            )

            Text("Status: $status", style = MaterialTheme.typography.bodyMedium)

            if (!viewModel.canAdvertise()) {
                Text("This phone does not report BLE advertising support.", color = MaterialTheme.colorScheme.error)
            }

            if (permissions.isNotEmpty()) {
                OutlinedButton(onClick = { launcher.launch(permissions) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Bluetooth permissions")
                }
            }

            Button(
                onClick = { if (active) viewModel.stop() else viewModel.start() },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.canAdvertise()
            ) {
                Icon(if (active) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (active) "Stop advertiser" else "Start advertiser")
            }

            Text(
                "Test: start advertising here, then scan with FlipperDroid V4 or another BLE scanner on your second phone. This is a normal BLE advertisement, not a flood.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
