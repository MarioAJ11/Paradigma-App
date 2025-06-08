package com.example.paradigmaapp.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define de forma segura y tipada cada uno de los destinos principales
 * de la barra de navegación inferior.
 * Al ser una 'sealed class', nos aseguramos de que solo pueden existir los objetos
 * definidos aquí dentro, lo que hace el código más robusto.
 *
 * @property route La ruta de navegación asociada al item (de la clase Screen).
 * @property title El texto que se mostrará como etiqueta del item.
 * @property icon El icono que representará al item en la barra.
 *
 * @author Mario Alguacil Juárez
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    /** Representa la pantalla de Inicio, que actúa como destino por defecto y reemplazo dinámico. */
    data object Home : BottomNavItem(Screen.Home.route, "Inicio", Icons.Default.Home)
    /** Representa la pantalla de Búsqueda. */
    data object Search : BottomNavItem(Screen.Search.route, "Buscar", Icons.Default.Search)
    /** Representa la pantalla de "Seguir Escuchando". */
    data object OnGoing : BottomNavItem(Screen.OnGoing.route, "Continuar", Icons.Default.AccessTimeFilled)
    /** Representa la pantalla de Descargas. */
    data object Downloads : BottomNavItem(Screen.Downloads.route, "Descargas", Icons.Default.Download)
    /** Representa la pantalla de la Cola de reproducción. */
    data object Queue : BottomNavItem(Screen.Queue.route, "Cola", Icons.AutoMirrored.Filled.List)
    /** Representa la pantalla de Ajustes. */
    data object Settings : BottomNavItem(Screen.Settings.route, "Ajustes", Icons.Default.Settings)
}