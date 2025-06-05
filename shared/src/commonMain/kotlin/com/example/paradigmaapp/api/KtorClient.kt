package com.example.paradigmaapp.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Configura y proporciona la instancia global del [HttpClient] de Ktor.
 * Este cliente se utiliza en toda la aplicación (desde el módulo `shared`)
 * para realizar las peticiones de red.
 *
 * Utiliza un motor HTTP específico para cada plataforma (Android/iOS) que se
 * provee a través de la función [provideHttpClientEngine].
 *
 * Incluye configuración para:
 * - Negociación de contenido JSON con kotlinx.serialization.
 * - Logging de peticiones y respuestas HTTP para depuración (nivel ALL por defecto).
 * - Timeouts para las peticiones HTTP, para evitar esperas indefinidas.
 *
 * @author Mario Alguacil Juárez
 */
val ktorClient = HttpClient(provideHttpClientEngine()) {

    // Instala el plugin ContentNegotiation para manejar automáticamente la
    // serialización/deserialización de objetos Kotlin a/desde JSON.
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignora claves en el JSON que no están en las data classes.
            prettyPrint = true       // Formatea el JSON en los logs para mejor legibilidad (desarrollo).
            isLenient = true         // Permite formatos JSON no estrictamente conformes.
            coerceInputValues = true // Intenta convertir tipos si es posible (ej. String "123" a Int).
            explicitNulls = false    // Los campos opcionales no requieren `null` explícitos en el JSON.
        })
    }

    // Instala el plugin Logging para registrar detalles de las peticiones y respuestas HTTP.
    // Muy útil durante el desarrollo y depuración.
    install(Logging) {
        logger = Logger.DEFAULT // Utiliza el logger por defecto (consola/Logcat).
        level = LogLevel.ALL    // Registra toda la información: headers, body, etc.
        // Considerar cambiar a LogLevel.INFO o LogLevel.NONE para producción.
    }

    // Instala y configura el plugin HttpTimeout para controlar los tiempos de espera.
    install(HttpTimeout) {
        // Tiempo máximo total para una petición HTTP completa.
        requestTimeoutMillis = 30000 // 30 segundos

        // Tiempo máximo para establecer una conexión con el servidor.
        connectTimeoutMillis = 15000 // 15 segundos

        // Tiempo máximo de inactividad entre la recepción de paquetes de datos.
        socketTimeoutMillis = 20000  // 20 segundos
    }
}