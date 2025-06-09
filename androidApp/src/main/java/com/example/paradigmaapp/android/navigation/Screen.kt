package com.example.paradigmaapp.android.navigation

import android.net.Uri

/**
 * Define las diferentes pantallas (rutas) de la aplicación para la navegación con Jetpack Compose.
 * Cada objeto representa un destino navegable. Para rutas con argumentos, se proporcionan
 * funciones `createRoute` para construir la ruta completa con los argumentos necesarios.
 *
 * @property route La cadena de ruta base para la pantalla.
 *
 * @author Mario Alguacil Juárez
 */
sealed class Screen(val route: String) {
    /** Pantalla de inicio de la aplicación. */
    object Onboarding : Screen("onboarding_screen")

    /** Pantalla principal que muestra la lista de programas. */
    object Home : Screen("home_screen")

    /** Pantalla para buscar episodios. */
    object Search : Screen("search_screen")

    /** Pantalla que muestra los episodios descargados. */
    object Downloads : Screen("downloads_screen")

    /** Pantalla que muestra la cola de reproducción. */
    object Queue : Screen("queue_screen")

    /** Pantalla que muestra los episodios en curso ("Seguir Escuchando"). */
    object OnGoing : Screen("on_going_screen")

    /**
     * Pantalla que muestra los detalles de un programa y sus episodios.
     * Requiere `programaId` y `programaNombre` como argumentos de navegación.
     */
    object Programa : Screen("programa_screen/{programaId}/{programaNombre}") {
        /**
         * Crea la ruta completa para navegar a la pantalla de un programa específico.
         * El nombre del programa se codifica para URL para manejar caracteres especiales.
         *
         * @param programaId El ID del programa.
         * @param programaNombre El nombre del programa.
         * @return La cadena de ruta formateada.
         */
        fun createRoute(programaId: Int, programaNombre: String): String {
            val encodedNombre = Uri.encode(programaNombre)
            return "programa_screen/$programaId/$encodedNombre"
        }
    }

    /**
     * Pantalla que muestra los detalles de un episodio específico.
     * Requiere `episodeId` como argumento de navegación.
     */
    object EpisodeDetail : Screen("episode_detail_screen/{episodeId}") {
        /**
         * Crea la ruta completa para navegar a la pantalla de detalle de un episodio.
         *
         * @param episodeId El ID del episodio.
         * @return La cadena de ruta formateada.
         */
        fun createRoute(episodeId: Int): String {
            return "episode_detail_screen/$episodeId"
        }
    }

    /** Pantalla de Ajustes de la aplicación. */
    object Settings : Screen("settings_screen")

    /** Pantalla del reproductor a pantalla completa. */
    object FullScreenPlayer : Screen("full_screen_player_screen")
}