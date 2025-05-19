package com.example.paradigmaapp.android.api

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Instancia global del [HttpClient] de Ktor configurada para Android.
 * Utilizada para realizar peticiones de red en la aplicación.
 *
 * Configura el plugin [ContentNegotiation] para la serialización y deserialización de JSON
 * utilizando la librería [kotlinx.serialization.json].
 *
 * @author Mario Alguacil Juárez
 */
val ktorClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Permite que el deserializador ignore claves desconocidas en el JSON.
            prettyPrint = true // Formatea el JSON al imprimirlo (útil para debugging).
            isLenient = true // Permite la deserialización de ciertos formatos JSON que no son estrictos.
        })
    }
}