package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.ThemeViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
/**
 * Affiche l ecran des parametres avec plusieurs sections configurables
 *
 * Cette interface permet de gerer les preferences liees a l apparence aux notifications au retour haptique et sonore ainsi qu au stockage
 * Les options incluent le theme sombre la persistance de l ecran les notifications les vibrations les sons la suppression du cache et l export des donnees
 *
 * @param navController controleur de navigation permettant de revenir a l ecran precedent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, themeViewModel: ThemeViewModel) {
    val darkMode by themeViewModel.isDarkMode.collectAsState()
    var notifications by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        supportingContent = { Text("Enable dark theme") },
                        leadingContent = {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { themeViewModel.setDarkMode(it) }
                            )
                        }
                    )
                }
            }

            item {
                Text(
                    "Help & Support",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val context = LocalContext.current
                    ListItem(
                        headlineContent = { Text("Manage permissions") },
                        supportingContent = { Text("Open system settings to manage the app's permissions.") },
                        leadingContent = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                            }
                        }
                    )
                }
            }
        }
    }
}
