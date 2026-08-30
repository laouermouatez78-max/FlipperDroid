package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.ValueCard

@Composable
fun NetworkScreen(viewModel: AppViewModel) {
    val network by viewModel.network.collectAsState()
    val probe by viewModel.probe.collectAsState()
    val multiProbe by viewModel.multiProbe.collectAsState()
    var host by remember { mutableStateOf("example.com") }
    var port by remember { mutableStateOf("443") }
    var portList by remember { mutableStateOf("22,80,443") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                "Diagnostics réseau",
                "Vue de l'interface active et tests strictement ciblés sur l'hôte/port saisi. Aucun balayage automatique de sous-réseau."
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Connexion active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = viewModel::refreshNetwork) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                }
            }
        }
        item { ValueCard("Transport", network.transport) }
        item { ValueCard("Interface", network.interfaceName ?: "Non déterminée") }
        item { ValueCard("Adresses locales", network.addresses.joinToString().ifBlank { "Aucune" }) }
        item { ValueCard("DNS", network.dnsServers.joinToString().ifBlank { "Non exposé" }) }
        item { ValueCard("Passerelle(s)", network.gateways.joinToString().ifBlank { "Non exposée" }) }
        item {
            ValueCard(
                "État",
                "Validée: ${if (network.validated) "oui" else "non"} · Réseau mesuré: ${if (network.metered) "oui" else "non"}"
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Test ciblé", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it.take(253) },
                        label = { Text("Hôte / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5) },
                        label = { Text("Port TCP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.resolveHost(host) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Dns, null)
                            Spacer(Modifier.width(6.dp))
                            Text("DNS")
                        }
                        Button(
                            onClick = { port.toIntOrNull()?.let { viewModel.tcpProbe(host, it) } },
                            enabled = port.toIntOrNull()?.let { it in 1..65535 } == true,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, null)
                            Spacer(Modifier.width(6.dp))
                            Text("TCP")
                        }
                    }
                    probe?.let {
                        HorizontalDivider()
                        Text(it.target, fontWeight = FontWeight.SemiBold)
                        Text(it.detail)
                        it.latencyMs?.let { ms -> Text("${ms} ms", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sondage multi-ports (même hôte)", fontWeight = FontWeight.Bold)
                    Text(
                        "Teste plusieurs ports sur l'hôte ci-dessus en une action, toujours limité à cette seule cible — jamais un balayage de sous-réseau.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = portList,
                        onValueChange = { portList = it.filter { c -> c.isDigit() || c == ',' }.take(60) },
                        label = { Text("Ports (séparés par des virgules, 16 max)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Web" to "80,443",
                            "Admin" to "22,3389,5900",
                            "Mail" to "25,110,143,993,995",
                            "Bases de données" to "1433,1521,3306,5432,6379,27017"
                        ).forEach { (label, preset) ->
                            AssistChip(onClick = { portList = preset }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    val parsedPorts = remember(portList) {
                        portList.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..65535 }
                    }
                    Button(
                        onClick = { viewModel.tcpProbeMultiple(host, parsedPorts) },
                        enabled = parsedPorts.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Speed, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sonder ${parsedPorts.size} port(s) sur $host")
                    }
                    multiProbe?.let { summary ->
                        HorizontalDivider()
                        if (summary.results.isEmpty()) {
                            Text("Sondage en cours…", style = MaterialTheme.typography.bodySmall)
                        } else {
                            summary.results.forEach { result ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(result.target, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if (result.success) "ouvert" else "fermé/filtré",
                                        color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
