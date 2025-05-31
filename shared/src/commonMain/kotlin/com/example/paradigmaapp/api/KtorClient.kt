package com.example.paradigmaapp.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout // Importar el plugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val ktorClient = HttpClient(provideHttpClientEngine()) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        })
    }

    // Logging para peticiones y respuestas Ktor.
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL // Mantenemos LogLevel.ALL para depuración
    }

    // Instalar y configurar HttpTimeout
    install(HttpTimeout) {
        requestTimeoutMillis = 30000 // Tiempo total para una petición, incluyendo conexión y lectura (30 segundos)
        connectTimeoutMillis = 15000 // Tiempo para establecer una conexión (15 segundos)
        socketTimeoutMillis = 20000  // Tiempo máximo entre paquetes de datos una vez conectado (20 segundos)
    }
}