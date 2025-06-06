package com.example.paradigmaapp.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Composable que representa la barra de navegación inferior de la aplicación.
 * Muestra los diferentes destinos principales (Buscar, Continuar, Descargas, Cola, Ajustes)
 * y resalta el ítem actualmente seleccionado basado en la ruta de navegación activa.
 *
 * @param navController El [NavHostController] utilizado para determinar la ruta actual y
 * permitir la navegación.
 * @param onSearchClick Lambda que se invoca al pulsar el ítem de "Buscar".
 * @param onOnGoingClick Lambda que se invoca al pulsar el ítem de "Continuar".
 * @param onDownloadedClick Lambda que se invoca al pulsar el ítem de "Descargas".
 * @param onQueueClick Lambda que se invoca al pulsar el ítem de "Cola".
 * @param onSettingsClick Lambda que se invoca al pulsar el ítem de "Ajustes".
 * @param modifier Modificador opcional para aplicar al [NavigationBar].
 *
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
    // Observa la pila de retroceso actual para determinar el destino actual.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Define los colores para los items de la barra de navegación, usando el esquema de colores del tema.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, // Color del icono cuando está seleccionado
        selectedTextColor = MaterialTheme.colorScheme.primary, // Color del texto cuando está seleccionado
        indicatorColor = MaterialTheme.colorScheme.primaryContainer, // Color del indicador de selección (fondo del ítem)
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Color del icono cuando no está seleccionado
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Color del texto cuando no está seleccionado
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface, // Color de fondo de la barra de navegación
        contentColor = MaterialTheme.colorScheme.onSurface // Color de contenido por defecto (raramente usado directamente aquí)
    ) {
        // Ítem de Búsqueda
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
            label = { Text("Buscar") }, // Añadir etiqueta de texto
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Search.route } == true,
            onClick = onSearchClick,
            colors = itemColors // Aplicar colores personalizados
        )
        // Ítem de Continuar (OnGoing)
        NavigationBarItem(
            icon = { Icon(Icons.Filled.AccessTimeFilled, contentDescription = "Continuar") },
            label = { Text("Continuar") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.OnGoing.route } == true,
            onClick = onOnGoingClick,
            colors = itemColors
        )
        // Ítem de Descargas
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Download, contentDescription = "Descargas") },
            label = { Text("Descarga") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Downloads.route } == true,
            onClick = onDownloadedClick,
            colors = itemColors
        )
        // Ítem de Cola
        NavigationBarItem(
            icon = { Icon(Icons.Filled.List, contentDescription = "Cola") },
            label = { Text("Cola") },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Queue.route } == true,
            onClick = onQueueClick,
            colors = itemColors
        )
        // Ítem de Ajustes
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes") },
            // El ítem de Ajustes no se marca como "seleccionado" de la misma manera que las pantallas principales,
            // ya que es una pantalla a la que se navega, no una pestaña persistente del mismo nivel.
            // Si Screen.Settings.route fuera una ruta de alto nivel en la barra, esta lógica cambiaría.
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
            onClick = onSettingsClick,
            colors = itemColors
        )
    }
}