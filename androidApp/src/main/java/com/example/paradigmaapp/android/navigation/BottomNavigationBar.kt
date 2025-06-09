package com.example.paradigmaapp.android.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.paradigmaapp.android.ui.BottomNavItem
import com.example.paradigmaapp.android.ui.navigateToScreenIfDifferent

/**
 * Composable que representa la barra de navegación inferior con comportamiento dinámico.
 * El ítem correspondiente a la pantalla activa se reemplaza por un botón "Inicio".
 *
 * @param navController El [NavHostController] para gestionar la navegación y saber la ruta actual.
 * @param modifier El [Modifier] que se aplicará al contenedor.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // La lista de todos los items que componen nuestra barra de navegación.
    val navItems = listOf(
        BottomNavItem.Search,
        BottomNavItem.OnGoing,
        BottomNavItem.Downloads,
        BottomNavItem.Queue,
        BottomNavItem.Settings
    )

    // Observamos la pila de navegación para saber cuál es la ruta activa.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        // Iteramos sobre cada uno de los items definidos.
        navItems.forEach { item ->
            // Comprobamos si la ruta del item actual en el bucle es la pantalla activa.
            val isSelected = currentRoute == item.route

            if (isSelected) {
                // SI ES LA PANTALLA ACTIVA: Dibujamos el botón "Inicio" en su lugar.
                NavigationBarItem(
                    icon = { Icon(BottomNavItem.Home.icon, contentDescription = BottomNavItem.Home.title) },
                    label = { Text(BottomNavItem.Home.title) },
                    selected = false, // "Inicio" es una acción, no una selección.
                    onClick = {
                        // Navega a Inicio reseteando la pila de navegación.
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                // SI NO ES LA PANTALLA ACTIVA: Dibujamos el botón normal del item.
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = false, // En esta UI, ningún item se marca como seleccionado.
                    onClick = {
                        // Navegamos a la pantalla del item.
                        navigateToScreenIfDifferent(navController, item.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}