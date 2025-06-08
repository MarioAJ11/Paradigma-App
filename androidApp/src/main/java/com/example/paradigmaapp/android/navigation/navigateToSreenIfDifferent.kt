package com.example.paradigmaapp.android.navigation

import androidx.navigation.NavHostController


/**
 * Función de utilidad para la navegación de la barra inferior.
 * Comprueba si la ruta actual es diferente de la ruta de destino antes de navegar,
 * para evitar añadir la misma pantalla a la pila de navegación múltiples veces.
 * Gestiona la pila de retroceso para un comportamiento de navegación estándar en la barra inferior.
 *
 * @param navController El [NavHostController] para la navegación.
 * @param route La ruta a la que se quiere navegar.
 */
fun navigateToScreenIfDifferent(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route != route) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}