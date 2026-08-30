package com.example.flipperdroid.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.LogEntry
import com.example.flipperdroid.ui.components.HeroBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(viewModel: AppViewModel) {
    val logs by viewModel.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var moduleFilter by remember { mutableStateOf("Tous") }
    val modules = remember(logs) { listOf("Tous") + logs.map { it.module }.distinct().sorted() }
    val visible = remember(logs, moduleFilter) {
        if (moduleFilter == "Tous") logs else logs.filter { it.module == moduleFilter }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { HeroBanner("Journal local", "Historique des actions et diagnostics. Conservé uniquement en mémoire pendant l'exécution de l'application.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(viewModel.logsAsText())) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copier")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid logs")
                            putExtra(Intent.EXTRA_TEXT, viewModel.logsAsText())
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager les logs"))
                    },
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partager")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid logs (JSON)")
                            putExtra(Intent.EXTRA_TEXT, viewModel.logsAsJson())
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager les logs (JSON)"))
                    },
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.DataObject, contentDescription = "Exporter en JSON")
                }
                IconButton(onClick = viewModel::clearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Vider")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                modules.take(6).forEach { module ->
                    FilterChip(
                        selected = moduleFilter == module,
                        onClick = { moduleFilter = module },
                        label = { Text(module) }
                    )
                }
            }
        }
        item { Text("${visible.size} entrée(s)", fontWeight = FontWeight.SemiBold) }
        items(visible) { entry -> LogRow(entry) }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val time = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$time  [${entry.module}]", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text(entry.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
