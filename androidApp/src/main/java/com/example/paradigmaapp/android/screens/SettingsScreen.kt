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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.HelpItem
import com.example.paradigmaapp.android.ui.ListDivider
import com.example.paradigmaapp.android.ui.SettingItemRow
import com.example.paradigmaapp.android.ui.ThemeSelectionButton
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel

/**
 * Composable que renderiza la pantalla de Ajustes y la Guía de Ayuda de la aplicación.
 * Permite al usuario configurar preferencias y consultar una guía detallada.
 *
 * @param settingsViewModel El ViewModel que proporciona y gestiona el estado de los ajustes.
 * @param onBackClick Lambda que se invoca cuando el usuario pulsa el botón de retroceso.
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
    val websiteUrl = settingsViewModel.mainWebsiteUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y Ayuda") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Preferencias
            Text(text = "Preferencias", style = MaterialTheme.typography.titleLarge)
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

            // Sección de Apariencia
            Text(text = "Apariencia", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(false) },
                    text = "Claro",
                    isSelected = isManuallySetToDarkTheme == false,
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(true) },
                    text = "Oscuro",
                    isSelected = isManuallySetToDarkTheme == true,
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(null) },
                    text = "Sistema",
                    isSelected = isManuallySetToDarkTheme == null,
                    modifier = Modifier.weight(1f)
                )
            }
            ListDivider()

            // --- SECCIONES DE AYUDA RESTAURADAS ---
            Text(text = "Ayuda y Funcionalidades", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.TouchApp,
                title = "Ver Detalles del Episodio",
                description = "Mantén pulsado cualquier episodio en una lista para ver su pantalla de detalles completa."
            )
            HelpItem(
                icon = Icons.Default.OpenInFull,
                title = "Reproductor Ampliado",
                description = "Pulsa sobre la información del episodio en el reproductor inferior para abrir la vista a pantalla completa."
            )
            ListDivider()

            Text(text = "Controles del Reproductor", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.PlayCircle,
                title = "Reproducir / Pausar",
                description = "El botón central grande inicia o detiene la reproducción del contenido actual."
            )
            HelpItem(
                icon = Icons.Default.VolumeUp,
                title = "Control de Volumen",
                description = "Pulsa el icono del altavoz para mostrar y ajustar el nivel del volumen."
            )
            HelpItem(
                icon = Icons.Default.Podcasts,
                title = "Radio en Directo",
                description = "El botón de la antena activa o desactiva la radio en directo. Se pondrá de color gris cuando esté activa."
            )
            ListDivider()

            Text(text = "Menú de Navegación", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.Home,
                title = "Inicio",
                description = "El icono de la pestaña activa se convierte en 'Inicio' para volver rápidamente a la lista de programas."
            )
            HelpItem(icon = Icons.Default.Search, title = "Buscar", description = "Encuentra cualquier episodio por título o descripción.")
            HelpItem(icon = Icons.Default.History, title = "Continuar", description = "Aquí aparecen los episodios que has empezado a escuchar pero no has terminado.")
            HelpItem(icon = Icons.Default.Download, title = "Descargas", description = "Accede a los episodios guardados para escucharlos sin conexión.")
            HelpItem(icon = Icons.AutoMirrored.Filled.List, title = "Cola", description = "Organiza una lista de reproducción con los episodios que quieres escuchar a continuación.")
            HelpItem(icon = Icons.Default.Settings, title = "Ajustes", description = "Configura las preferencias de la aplicación y consulta esta ayuda.")
            ListDivider()

            Text(text = "Opciones adicionales episodios", style = MaterialTheme.typography.titleLarge)
            HelpItem(icon = Icons.Default.Download, title = "Descargar Episodio", description = "Desde los tres puntitos o la vista detalla del episodio, puedes descargarlo o eliminarlo a tu dispositivo.")
            HelpItem(icon = Icons.AutoMirrored.Filled.List, title = "Añadir a cola", description = "Desde los tres puntitos o la vista detalla del episodio, puedes añadirlo o elimnarlo a la cola de reproducción.")
            ListDivider()


            // Sección de Más Información
            Text(text = "Más Información", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = { uriHandler.openUri(websiteUrl) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Visitar web de Paradigma Media")
            }

            Spacer(modifier = Modifier.height(166.dp))
        }
    }
}