package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.AppViewModel
import com.example.flipperdroid.model.Accent
import com.example.flipperdroid.model.ThemeMode
import com.example.flipperdroid.ui.components.HeroBanner

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeroBanner("Réglages", "Une interface adaptable, avec confidentialité activée par défaut pour masquer les adresses radio à l'écran.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SettingsBrightness, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Apparence", fontWeight = FontWeight.Bold)
                    }
                    Text("Thème", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text("Système") },
                            leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text("Sombre") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null) }
                        )
                        FilterChip(
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text("Clair") },
                            leadingIcon = { Icon(Icons.Default.LightMode, null) }
                        )
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Accent", fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Accent.entries.forEach { accent ->
                            FilterChip(
                                selected = settings.accent == accent,
                                onClick = { viewModel.setAccent(accent) },
                                label = {
                                    Text(
                                        when (accent) {
                                            Accent.MINT -> "Mint"
                                            Accent.CYAN -> "Cyan"
                                            Accent.AMBER -> "Ambre"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Mode confidentialité", fontWeight = FontWeight.Bold)
                        Text("Masque partiellement MAC/BSSID dans l'interface et les captures d'écran.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.privacyMode, onCheckedChange = viewModel::setPrivacy)
                }
            }
        }
        item {
            Text(
                "Aucune télémétrie ni compte utilisateur. Les tests réseau ne partent qu'après une action explicite.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
