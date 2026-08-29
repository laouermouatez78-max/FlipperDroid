package com.example.flipperdroid.view

import android.content.Context
import android.hardware.ConsumerIrManager
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class CapabilityStatus(
    val name: String,
    val value: String,
    val available: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusScreen(navController: NavController) {
    val context = LocalContext.current
    val statuses = remember {
        val nfc = NfcAdapter.getDefaultAdapter(context)
        val ir = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        listOf(
            CapabilityStatus(
                name = "Android",
                value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ),
            CapabilityStatus(
                name = "NFC hardware",
                value = when {
                    nfc == null -> "Not available"
                    nfc.isEnabled -> "Available and enabled"
                    else -> "Available but disabled"
                },
                available = nfc != null
            ),
            CapabilityStatus(
                name = "Infrared emitter",
                value = if (ir?.hasIrEmitter() == true) "Available" else "Not available",
                available = ir?.hasIrEmitter() == true
            ),
            CapabilityStatus(
                name = "Network",
                value = if (connectivity.activeNetwork != null) "Active network detected" else "No active network",
                available = connectivity.activeNetwork != null
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Status · V2") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Quick compatibility check for the features exposed by FlipperDroid V2.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            statuses.forEach { status ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (status.available) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (status.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(status.name, fontWeight = FontWeight.Bold)
                                Text(status.value, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
