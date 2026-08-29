package com.example.flipperdroid.view

import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfraredScreen(navController: NavController) {
    val context = LocalContext.current
    val irManager = remember { context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager }
    val hasIr = irManager?.hasIrEmitter() == true
    val ranges = remember { runCatching { irManager?.carrierFrequencies?.toList().orEmpty() }.getOrDefault(emptyList()) }

    var frequencyText by remember { mutableStateOf("38000") }
    var rawPattern by remember { mutableStateOf("9000,4500,560,560,560,1690,560,560") }
    var necAddress by remember { mutableStateOf("00") }
    var necCommand by remember { mutableStateOf("00") }
    var status by remember { mutableStateOf(if (hasIr) "IR emitter ready" else "No IR emitter detected") }

    fun transmit(pattern: IntArray) {
        val frequency = frequencyText.toIntOrNull()
        if (!hasIr || irManager == null) {
            status = "This phone has no Consumer IR emitter"
            return
        }
        if (frequency == null || frequency !in 20000..60000) {
            status = "Use a carrier frequency between 20000 and 60000 Hz"
            return
        }
        if (pattern.isEmpty() || pattern.size > 4096 || pattern.any { it <= 0 } || pattern.sumOf { it.toLong() } > 1_500_000L) {
            status = "Invalid IR pattern (max 4096 timings / 1.5 s)"
            return
        }
        runCatching { irManager.transmit(frequency, pattern) }
            .onSuccess { status = "IR transmitted · ${pattern.size} timings @ $frequency Hz" }
            .onFailure { status = "IR transmit failed: ${it.message}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IR Transmitter · V4") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsRemote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Real Consumer IR", fontWeight = FontWeight.Bold)
                        Text(status)
                    }
                }
            }

            if (ranges.isNotEmpty()) {
                Text(
                    "Phone carrier ranges: " + ranges.joinToString { "${it.minFrequency}-${it.maxFrequency} Hz" },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = frequencyText,
                onValueChange = { frequencyText = it.filter(Char::isDigit).take(5) },
                label = { Text("Carrier frequency (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Raw pattern", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = rawPattern,
                onValueChange = { rawPattern = it },
                label = { Text("mark,space,mark,space… µs") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
            Button(
                onClick = {
                    val parsed = rawPattern.split(',', ' ', ';', '\n')
                        .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
                        .toIntArray()
                    transmit(parsed)
                },
                enabled = hasIr,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Transmit raw IR pattern") }

            HorizontalDivider()
            Text("NEC frame generator", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = necAddress,
                    onValueChange = { necAddress = it.uppercase().filter { c -> c.isDigit() || c in 'A'..'F' }.take(2) },
                    label = { Text("Address hex") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = necCommand,
                    onValueChange = { necCommand = it.uppercase().filter { c -> c.isDigit() || c in 'A'..'F' }.take(2) },
                    label = { Text("Command hex") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Button(
                onClick = {
                    val address = necAddress.toIntOrNull(16)
                    val command = necCommand.toIntOrNull(16)
                    if (address == null || command == null) status = "Enter 00-FF NEC address and command"
                    else {
                        frequencyText = "38000"
                        transmit(buildNecPattern(address, command))
                    }
                },
                enabled = hasIr,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Transmit NEC frame") }

            Text(
                "IR codes differ by device model. Use codes captured/documented for equipment you control. V4 sends exactly the pattern you provide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildNecPattern(address: Int, command: Int): IntArray {
    val bytes = intArrayOf(address and 0xFF, address.inv() and 0xFF, command and 0xFF, command.inv() and 0xFF)
    val timings = mutableListOf(9000, 4500)
    for (byte in bytes) {
        for (bit in 0 until 8) {
            timings += 560
            timings += if ((byte shr bit) and 1 == 1) 1690 else 560
        }
    }
    timings += 560
    return timings.toIntArray()
}
