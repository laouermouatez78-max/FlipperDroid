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
    val realProfile by viewModel.realProfile.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsState()
    val realSessionCount by viewModel.realSessionCount.collectAsState()
    val lastSessionSummary by viewModel.lastSessionSummary.collectAsState()
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val hasPermissions = remember(permissionRefresh) { viewModel.permissionsGranted() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionRefresh++ }

    val checkedStates = remember(advertisementSets) {
        mutableStateListOf<Boolean>().apply { addAll(List(advertisementSets.size) { true }) }
    }

    LaunchedEffect(checkedStates.toList()) { viewModel.setCheckedPayloads(checkedStates.toList()) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSimulation()
            viewModel.stopRealBeacon()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Radio Lab · V5") },
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Bounded real-radio resilience test", fontWeight = FontWeight.Bold)
                            Text(
                                "One identifiable FD5LAB advertiser, automatic stop, 15-second cooldown, and local-only preview for vendor/crash-oriented catalogue entries.",
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
                        Text("Test profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BleSpamViewModel.RealTestProfile.entries.forEach { profile ->
                                FilterChip(
                                    selected = realProfile == profile,
                                    onClick = { viewModel.setRealProfile(profile) },
                                    enabled = !realActive && cooldownSeconds == 0,
                                    label = { Text(profile.label) }
                                )
                            }
                        }
                        Text(
                            if (realProfile == BleSpamViewModel.RealTestProfile.SHORT_BURST_10S) {
                                "Low TX-power 10-second test session. The payload stays in the identifiable FD5LAB namespace."
                            } else {
                                "Balanced 30-second test session, followed by automatic stop and cooldown."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = realPayload,
                            onValueChange = viewModel::updateRealPayload,
                            enabled = !realActive && cooldownSeconds == 0,
                            singleLine = true,
                            label = { Text("Lab marker") },
                            supportingText = { Text("Up to 8 characters · always prefixed FD5LAB · manufacturer ID 0xFFFF") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(realStatus, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (realActive) Text("Automatic stop in ${remainingSeconds}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                if (!realActive && cooldownSeconds > 0) Text("Cooldown: ${cooldownSeconds}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                Text("Started sessions: $realSessionCount", style = MaterialTheme.typography.bodySmall)
                                Text("Last: $lastSessionSummary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!hasPermissions && viewModel.requiredPermissions().isNotEmpty()) {
                            OutlinedButton(onClick = { permissionLauncher.launch(viewModel.requiredPermissions()) }, modifier = Modifier.fillMaxWidth()) {
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
                            enabled = realActive || (cooldownSeconds == 0 && viewModel.canAdvertise() && (hasPermissions || viewModel.requiredPermissions().isEmpty())),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (realActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    realActive -> "Emergency stop"
                                    cooldownSeconds > 0 -> "Cooldown ${cooldownSeconds}s"
                                    realProfile == BleSpamViewModel.RealTestProfile.SHORT_BURST_10S -> "Start controlled 10 s test"
                                    else -> "Start controlled 30 s test"
                                }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("LEGACY PROFILE PREVIEW", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Apple/Samsung legacy and crash-oriented catalogue entries stay local preview only. They are never broadcast by the V5 real-radio path.",
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
                            leadingIcon = if (brand == value) { { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Payload catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text("${checkedStates.count { it }}/${checkedStates.size} selected") })
                }
            }

            items(advertisementSets.size) { index ->
                val set = advertisementSets[index]
                Surface(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = checkedStates.getOrNull(index) == true,
                        enabled = !isActive,
                        onValueChange = { checked -> checkedStates[index] = checked }
                    ),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checkedStates.getOrNull(index) == true, enabled = !isActive, onCheckedChange = null)
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
                    Text(if (isActive) "Stop profile preview" else "Preview selected profiles locally")
                }
            }

            item {
                Text("Lab log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 240.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (logs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No BLE events yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            logs.takeLast(16).forEach { Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }
            }
        }
    }
}
