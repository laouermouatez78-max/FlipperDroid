package com.example.flipperdroid.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V4AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About FlipperDroid V4") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Icon(
                    Icons.Default.Android,
                    contentDescription = "FlipperDroid V4",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Text(
                    "FlipperDroid V4",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }

            item {
                Text(
                    "Version 4.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Text(
                    "Android security and hardware toolkit for devices, tags and networks you own or are explicitly authorized to test.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("V4 modules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        ListItem(headlineContent = { Text("NFC / RFID") }, leadingContent = { Icon(Icons.Default.Nfc, null) })
                        ListItem(headlineContent = { Text("BLE Scanner / Advertiser") }, leadingContent = { Icon(Icons.Default.Bluetooth, null) })
                        ListItem(headlineContent = { Text("Wi‑Fi Audit") }, leadingContent = { Icon(Icons.Default.Wifi, null) })
                        ListItem(headlineContent = { Text("LAN Analyzer") }, leadingContent = { Icon(Icons.Default.Lan, null) })
                        ListItem(headlineContent = { Text("USB Inspector") }, leadingContent = { Icon(Icons.Default.Usb, null) })
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Owner Mode") },
                        supportingContent = { Text("Use active diagnostics only on systems you control or have permission to assess.") },
                        leadingContent = { Icon(Icons.Default.Security, null) }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Build channel") },
                    supportingContent = { Text("FlipperDroid V4 · 4.0.0") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}
