package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel

/**
 * Pantalla de Ajustes de la aplicación.
 * Permite al usuario configurar opciones como el tema de la aplicación (claro/oscuro/sistema),
 * la preferencia de inicio con radio en directo y acceder a enlaces externos.
 *
 * @param settingsViewModel El [SettingsViewModel] que gestiona el estado y la lógica de los ajustes.
 * @param onBackClick Lambda que se invoca cuando el usuario pulsa el botón de retroceso en la TopAppBar.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val isStreamActive by settingsViewModel.isStreamActive.collectAsState()
    val isManuallySetToDarkTheme by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sección: Preferencia de Streaming
            SettingItemRow(
                title = "Abrir con Radio en Directo",
                description = "Iniciar la app con el stream de Andaina FM activo."
            ) {
                Switch(
                    checked = isStreamActive,
                    onCheckedChange = { settingsViewModel.toggleStreamActive() }
                )
            }
            ListDivider()

            // Sección: Tema de la Aplicación
            Text(
                text = "Tema de la Aplicación",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingItemRow(title = "Tema Oscuro") {
                Switch(
                    checked = isManuallySetToDarkTheme == true,
                    onCheckedChange = { isChecked ->
                        settingsViewModel.setThemePreference(if (isChecked) true else false)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { settingsViewModel.setThemePreference(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = if (isManuallySetToDarkTheme == null) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                },
                border = if (isManuallySetToDarkTheme != null) ButtonDefaults.outlinedButtonBorder else null
            ) {
                Text("Seguir tema del sistema")
            }
            ListDivider()

            // Sección: Más Información
            Text(
                text = "Más Información",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = { uriHandler.openUri("https://paradigmamedia.org/") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Visitar web de Paradigma Media")
            }
        }
    }
}

/**
 * Composable auxiliar para estructurar cada ítem de ajuste.
 *
 * @param title Título del ajuste.
 * @param description Descripción opcional.
 * @param control Composable del control (ej. Switch).
 */
@Composable
private fun SettingItemRow(
    title: String,
    description: String? = null,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        control()
    }
}

/** Composable para un Divider con padding vertical. */
@Composable
private fun ListDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}