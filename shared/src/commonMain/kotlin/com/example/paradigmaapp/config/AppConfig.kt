package com.example.paradigmaapp.config

import kotlinx.serialization.Serializable

/**
 * Representa la estructura del fichero de configuración remota.
 * Contiene todas las URLs y valores que pueden ser actualizados desde el servidor.
 */
@Serializable
data class AppConfig(
    val wordpressApiBaseUrl: String,
    val mainWebsiteUrl: String,
    val liveStreamApiUrl: String,
    val liveStreamUrl: String
)