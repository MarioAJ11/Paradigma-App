package com.example.paradigmaapp.android.navigation

/**
 * Define las diferentes pantallas (rutas) en la aplicación para la navegación.
 * Cada objeto representa un destino navegable.
 * @author Mario Alguacil Juárez
 */
sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Search : Screen("search_screen")
    object Downloads : Screen("downloads_screen")
    object Queue : Screen("queue_screen")
    object OnGoing : Screen("on_going_screen")
    // Settings puede ser un diálogo o una pantalla, si es pantalla:
    // object Settings : Screen("settings_screen")
}