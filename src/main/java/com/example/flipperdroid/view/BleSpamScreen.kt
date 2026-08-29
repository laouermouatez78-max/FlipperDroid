package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import com.example.flipperdroid.viewmodel.BleSpamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleSpamScreen(
    navController: NavController? = null,
    viewModel: BleSpamViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val advertisementSets by viewModel.advertisementSets.collectAsState()
    val isActive by viewModel.isActive.collectAsState()
    val brand by viewModel.brand.collectAsState()
    val logs by viewModel.spamLogs.collectAsState()

    val checkedStates = remember(advertisementSets) {
        mutableStateListOf<Boolean>().apply {
            addAll(List(advertisementSets.size) { true })
        }
    }

    LaunchedEffect(checkedStates.toList()) {
        viewModel.setCheckedPayloads(checkedStates.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Lab") },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Column {
                        Text("Controlled simulation", fontWeight = FontWeight.Bold)
                        Text("Legacy Apple/Samsung BLE payload profiles are restored for analysis and training. V3 previews the rotation locally instead of flooding nearby devices.")
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    BleSpamViewModel.BleSpamBrand.APPLE to "Apple",
                    BleSpamViewModel.BleSpamBrand.SAMSUNG to "Samsung",
                    BleSpamViewModel.BleSpamBrand.ALL to "All"
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier.selectable(
                            selected = brand == value,
                            enabled = !isActive,
                            onClick = { viewModel.setBrand(value) }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = brand == value,
                            enabled = !isActive,
                            onClick = { viewModel.setBrand(value) }
                        )
                        Text(label)
                    }
                }
            }

            Text("Payload catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(advertisementSets.size) { index ->
                    val set = advertisementSets[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checkedStates.getOrNull(index) == true,
                                enabled = !isActive,
                                onValueChange = { checked -> checkedStates[index] = checked }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedStates.getOrNull(index) == true,
                            enabled = !isActive,
                            onCheckedChange = null
                        )
                        Text(set.title.ifBlank { set.type.toString() }, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Button(
                onClick = { viewModel.startSpam() },
                enabled = !isActive && checkedStates.any { it },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run BLE simulation")
            }

            if (isActive) {
                OutlinedButton(
                    onClick = { viewModel.stopSpam() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop simulation")
                }
            }

            Text("Simulation log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                items(logs.size) { index ->
                    Text(logs[index], style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
