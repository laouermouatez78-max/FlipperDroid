package com.example.flipperdroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flipperdroid.ui.components.HeroBanner
import com.example.flipperdroid.ui.components.ValueCard

@Composable
fun AboutScreen() {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroBanner("FlipperDroid 5.0", "Refonte complète orientée diagnostic, pentest autorisé et laboratoire mobile.") }
        item { ValueCard("Version", "5.0.0 · code 5") }
        item { ValueCard("Android", "API 24+ · cible API 36 · Jetpack Compose / Material 3") }
        item { ValueCard("Confidentialité", "Pas de télémétrie, pas de compte, pas de backend. Les réglages restent dans SharedPreferences.") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Principe", fontWeight = FontWeight.Bold)
                    Text(
                        "Les fonctionnalités actives sont explicites, limitées et orientées test. Les actions susceptibles de perturber des appareils ou réseaux tiers ne sont pas automatisées dans cette branche.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
