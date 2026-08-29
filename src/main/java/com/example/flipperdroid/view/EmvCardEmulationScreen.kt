package com.example.flipperdroid.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flipperdroid.viewmodel.EmvCardEmulationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvCardEmulationScreen(
    navController: NavController,
    viewModel: EmvCardEmulationViewModel
) {
    val isActive by viewModel.isEmulationActive.collectAsState()
    val cardType by viewModel.selectedCardType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMV Card Emulation") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.CreditCard, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Activez l'émulation pour transformer votre téléphone en carte EMV (Visa).", style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Émulation : ", modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = isActive,
                    onCheckedChange = { viewModel.setEmulationActive(it) }
                )
            }
            Text("Type de carte : $cardType")
            // (Plus tard : Dropdown pour choisir le type de carte)
            if (isActive) {
                Text("Votre téléphone émule une carte $cardType.", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("L'émulation est désactivée.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
} 