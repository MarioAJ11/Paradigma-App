package com.example.paradigmaapp.android

/**
 * Clase de datos que representa un Podcast con todos los metadatos.
 *
 * @property title Título del podcast con duración incluida
 * @property url URL del archivo de audio
 * @property imageUrl URL de la imagen de portada (puede ser null)
 * @property duration Duración formateada (MM:SS)
 */
data class Podcast(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val duration: String
)