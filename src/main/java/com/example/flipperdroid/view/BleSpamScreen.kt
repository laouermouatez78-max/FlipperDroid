package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSimulation() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Payload Lab · V4") },
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
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(28.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Controlled local preview", fontWeight = FontWeight.Bold)
                        Text(
                            "Inspect Apple/Samsung legacy payload profiles and preview their rotation locally. V4 does not flood nearby BLE devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    BleSpamViewModel.BleSpamBrand.APPLE to "Apple",
                    BleSpamViewModel.BleSpamBrand.SAMSUNG to "Samsung",
                    BleSpamViewModel.BleSpamBrand.ALL to "All"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = brand == value,
                        onClick = { viewModel.setBrand(value) },
                        enabled = !isActive,
                        label = { Text(label) },
                        leadingIcon = if (brand == value) {
                            { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payload catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text("${checkedStates.count { it }}/${checkedStates.size} selected") })
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(advertisementSets.size) { index ->
                    val set = advertisementSets[index]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checkedStates.getOrNull(index) == true,
                                enabled = !isActive,
                                onValueChange = { checked -> checkedStates[index] = checked }
                            ),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedStates.getOrNull(index) == true,
                                enabled = !isActive,
                                onCheckedChange = null
                            )
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(set.title.ifBlank { set.type.toString() }, fontWeight = FontWeight.SemiBold)
                                Text(set.type.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { if (isActive) viewModel.stopSimulation() else viewModel.startSimulation() },
                enabled = isActive || checkedStates.any { it },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isActive) "Stop local preview" else "Run local preview")
            }

            Text("Preview log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 160.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (logs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No preview events yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        items(logs.size) { index ->
                            Text(logs[index], style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
