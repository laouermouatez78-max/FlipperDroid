package com.example.flipperdroid.view

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.osint.OsintFileAnalyzer
import com.example.flipperdroid.osint.OsintFileReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsintFileScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<OsintFileReport?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                report = withContext(Dispatchers.IO) { OsintFileAnalyzer.analyze(context, uri) }
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Intelligence · V5") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { report?.let { clipboard.setText(AnnotatedString(it.text)) } },
                        enabled = report != null
                    ) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy report") }
                    IconButton(
                        onClick = {
                            report?.let {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FlipperDroid File Intelligence · ${it.name}")
                                    putExtra(Intent.EXTRA_TEXT, it.text)
                                }
                                context.startActivity(Intent.createChooser(share, "Share file intelligence"))
                            }
                        },
                        enabled = report != null
                    ) { Icon(Icons.Default.Share, contentDescription = "Share report") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.FilePresent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Local file triage", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Analyze a file selected through Android's document picker. Hashing, signature detection, EXIF/package metadata and basic document hints stay on-device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Analyzing…" else "Choose file")
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("What is inspected", fontWeight = FontWeight.Bold)
                    Text("• SHA-256 and SHA-1", style = MaterialTheme.typography.bodySmall)
                    Text("• magic/signature versus extension and MIME", style = MaterialTheme.typography.bodySmall)
                    Text("• sample entropy", style = MaterialTheme.typography.bodySmall)
                    Text("• image EXIF camera/software/date metadata", style = MaterialTheme.typography.bodySmall)
                    Text("• presence of GPS/serial metadata without automatically revealing GPS coordinates", style = MaterialTheme.typography.bodySmall)
                    Text("• PDF metadata hints and JavaScript/embedded-file/encryption markers", style = MaterialTheme.typography.bodySmall)
                    Text("• Office/ZIP package properties, VBA marker and Android package markers", style = MaterialTheme.typography.bodySmall)
                }
            }

            report?.let { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (item.warning != null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold)
                        item.warning?.let { warning ->
                            AssistChip(
                                onClick = {},
                                label = { Text(warning) },
                                leadingIcon = { Icon(Icons.Default.WarningAmber, contentDescription = null) }
                            )
                        }
                        SelectionContainer(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            androidx.compose.foundation.rememberScrollState().let { scroll ->
                                Text(
                                    item.text,
                                    modifier = Modifier.fillMaxSize().verticalScroll(scroll),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } ?: run {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Privacy-first by default", fontWeight = FontWeight.Bold)
                        Text(
                            "No file is uploaded by this module. Precise EXIF location is deliberately not printed automatically, because metadata can expose where an image was created.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
