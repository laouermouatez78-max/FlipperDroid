package com.example.flipperdroid.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
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
    val realActive by viewModel.realBeaconActive.collectAsState()
    val realPayload by viewModel.realPayloadText.collectAsState()
    val realStatus by viewModel.realStatus.collectAsState()
    val realTxProfile by viewModel.realTxProfile.collectAsState()
    val burstDurationSeconds by viewModel.burstDurationSeconds.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val hasPermissions = remember(permissionRefresh) { viewModel.permissionsGranted() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefresh++
    }

    val checkedStates = remember(advertisementSets) {
        mutableStateListOf<Boolean>().apply {
            addAll(List(advertisementSets.size) { true })
        }
    }

    LaunchedEffect(checkedStates.toList()) {
        viewModel.setCheckedPayloads(checkedStates.toList())
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSimulation()
            viewModel.stopRealBeacon()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Red Team Lab · V4") },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
                                Icons.Default.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp).size(28.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Bounded real-radio resilience test", fontWeight = FontWeight.Bold)
                            Text(
                                "Runs an identifiable FlipperDroid BLE beacon with a platform-managed normal or short stress profile. Every test stops automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("REAL RADIO TEST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)

                        OutlinedTextField(
                            value = realPayload,
                            onValueChange = viewModel::updateRealPayload,
                            enabled = !realActive,
                            singleLine = true,
                            label = { Text("Test payload") },
                            supportingText = { Text("Up to 12 characters · manufacturer ID 0xFFFF · FD4 marker") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("RF profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = realTxProfile == BleSpamViewModel.RealTxProfile.NORMAL,
                                onClick = { viewModel.setRealTxProfile(BleSpamViewModel.RealTxProfile.NORMAL) },
                                enabled = !realActive,
                                label = { Text("Normal") }
                            )
                            FilterChip(
                                selected = realTxProfile == BleSpamViewModel.RealTxProfile.SHORT_STRESS,
                                onClick = { viewModel.setRealTxProfile(BleSpamViewModel.RealTxProfile.SHORT_STRESS) },
                                enabled = !realActive,
                                label = { Text("Short stress") }
                            )
                        }

                        Text("Automatic stop", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 20).forEach { seconds ->
                                FilterChip(
                                    selected = burstDurationSeconds == seconds,
                                    onClick = { viewModel.setBurstDurationSeconds(seconds) },
                                    enabled = !realActive,
                                    label = { Text("${seconds}s") }
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(realStatus, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text(
                                    "Profile: ${if (realTxProfile == BleSpamViewModel.RealTxProfile.NORMAL) "balanced" else "low-latency burst"} · elapsed ${elapsedSeconds}s / ${burstDurationSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!hasPermissions && viewModel.requiredPermissions().isNotEmpty()) {
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Grant Bluetooth permissions")
                            }
                        }

                        Button(
                            onClick = {
                                if (realActive) viewModel.stopRealBeacon() else viewModel.startRealBeacon()
                                permissionRefresh++
                            },
                            enabled = realActive || (viewModel.canAdvertise() && (hasPermissions || viewModel.requiredPermissions().isEmpty())),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (realActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (realActive) "Emergency stop" else "Start bounded RF test")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("LEGACY PROFILE PREVIEW", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Apple/Samsung legacy payloads stay local preview only. Use the catalogue to inspect and compare their structures without transmitting them as vendor accessories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
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
                                { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payload catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text("${checkedStates.count { it }}/${checkedStates.size} selected") })
                }
            }

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

            item {
                OutlinedButton(
                    onClick = { if (isActive) viewModel.stopSimulation() else viewModel.startSimulation() },
                    enabled = isActive || checkedStates.any { it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isActive) "Stop profile preview" else "Preview selected profiles")
                }
            }

            item {
                Text("Lab log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 220.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (logs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No BLE events yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            logs.takeLast(14).forEach {
                                Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
