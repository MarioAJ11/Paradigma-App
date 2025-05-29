package com.example.paradigmaapp.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Composable para la barra de navegación inferior de la aplicación.
 * Muestra los diferentes destinos principales y resalta el actualmente seleccionado.
 *
 * @param navController El NavHostController para determinar la ruta actual.
 * @param onSearchClick Lambda para la acción de clic en el ítem de Búsqueda.
 * @param onOnGoingClick Lambda para la acción de clic en el ítem de Continuar.
 * @param onDownloadedClick Lambda para la acción de clic en el ítem de Descargas.
 * @param onQueueClick Lambda para la acción de clic en el ítem de Cola.
 * @param onSettingsClick Lambda para la acción de clic en el ítem de Ajustes.
 * @param modifier Modificador opcional.
 * @author Mario Alguacil Juárez
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    onSearchClick: () -> Unit,
    onOnGoingClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observa la entrada actual en la pila de retroceso para obtener el destino actual.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Search.route } == true,
            onClick = onSearchClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.AccessTimeFilled, contentDescription = "Continuar") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.OnGoing.route } == true,
            onClick = onOnGoingClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Download, contentDescription = "Descargas") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Downloads.route } == true,
            onClick = onDownloadedClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.List, contentDescription = "Cola") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Queue.route } == true,
            onClick = onQueueClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
            selected = false,
            onClick = onSettingsClick
        )
    }
}