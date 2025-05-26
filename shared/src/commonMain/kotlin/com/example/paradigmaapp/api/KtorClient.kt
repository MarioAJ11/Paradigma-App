package com.example.paradigmaapp.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Instancia global del [HttpClient] de Ktor configurada para uso multplataforma.
 * Utiliza un motor HTTP específico de la plataforma proporcionado por [provideHttpClientEngine].
 *
 * @author Mario Alguacil Juárez
 */
val ktorClient = HttpClient(provideHttpClientEngine()) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignorar claves desconocidas en el JSON.
            prettyPrint = true // Formatear JSON para debugging.
            isLenient = true // Permitir formatos JSON no estrictos.
            coerceInputValues = true // Intentar coaccionar valores si es posible.
            explicitNulls = false // No fallar si un campo opcional es null en el JSON.
        })
    }

    // Logging para peticiones y respuestas Ktor.
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
}