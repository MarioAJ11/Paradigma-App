package com.example.paradigmaapp.android.navigation

import android.net.Uri

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
    // Nueva pantalla de detalle de episodio
    object EpisodeDetail : Screen("episode_detail_screen/{episodeId}") {
        fun createRoute(episodeId: Int): String {
            return "episode_detail_screen/$episodeId"
        }
    }
}