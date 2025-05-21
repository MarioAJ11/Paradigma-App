package com.example.paradigmaapp.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home // Si usas un ícono de Home para la búsqueda/principal
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BottomNavigationBar(
    onSearchClick: () -> Unit,
    onOnGoingClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.AccessTimeFilled, contentDescription = "Buscar") },
            label = { Text("Buscar") },
            selected = false,
            onClick = onSearchClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Continuar") },
            label = { Text("Continuar") },
            selected = false,
            onClick = onOnGoingClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Download, contentDescription = "Descargas") },
            label = { Text("Descargas") },
            selected = false,
            onClick = onDownloadedClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.List, contentDescription = "Cola") },
            label = { Text("Cola") },
            selected = false,
            onClick = onQueueClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes") },
            selected = false,
            onClick = onSettingsClick
        )
    }
}