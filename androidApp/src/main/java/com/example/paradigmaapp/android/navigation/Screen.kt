package com.example.paradigmaapp.android.navigation

import android.net.Uri

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
    object Programa : Screen("programa_screen/{programaId}/{programaNombre}") {
        fun createRoute(programaId: Int, programaNombre: String): String {
            val encodedNombre = Uri.encode(programaNombre)
            return "programa_screen/$programaId/$encodedNombre"
        }
    }
}