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
 * Configuro y proporciono mi instancia global del [HttpClient] de Ktor.
 * Este cliente se utiliza en toda la aplicación (desde el módulo `shared`)
 * para realizar las peticiones de red.
 * Utiliza un motor HTTP específico para cada plataforma (Android/iOS) que se
 * provee a través de la función `provideHttpClientEngine()`.
 *
 * Incluye configuración para:
 * - Negociación de contenido JSON con kotlinx.serialization.
 * - Logging de peticiones y respuestas HTTP para depuración.
 * - Timeouts para las peticiones HTTP, para evitar esperas indefinidas.
 *
 * @author Mario Alguacil Juárez
 */
val ktorClient = HttpClient(provideHttpClientEngine()) { // El motor se obtiene de forma multiplataforma.

    // Instalo el plugin ContentNegotiation para manejar automáticamente la serialización/deserialización
    // de objetos Kotlin a/desde JSON.
    install(ContentNegotiation) {
        json(Json {
            // ignoreUnknownKeys = true: Si el JSON de respuesta tiene claves que no están en mis data classes, las ignoro.
            ignoreUnknownKeys = true
            // prettyPrint = true: Formateo el JSON en los logs para que sea más legible (útil en depuración).
            prettyPrint = true
            // isLenient = true: Permito formatos JSON que no sean estrictamente conformes (ej. comillas innecesarias).
            isLenient = true
            // coerceInputValues = true: Intento convertir tipos de datos si es posible (ej. un string "123" a Int).
            coerceInputValues = true
            // explicitNulls = false: No espero `null` explícitos para campos opcionales si no están presentes en el JSON.
            // Si un campo es opcional (ej. `val miCampo: String? = null`) y no está en el JSON,
            // se deserializará como null sin error. Si fuera `true`, se esperaría un `"miCampo": null` explícito.
            explicitNulls = false
        })
    }

    // Instalo el plugin Logging para registrar los detalles de las peticiones y respuestas HTTP.
    // Esto es muy útil durante el desarrollo y la depuración para ver qué se envía y recibe.
    install(Logging) {
        logger = Logger.DEFAULT // Uso el logger por defecto que imprime a la consola/Logcat.
        level = LogLevel.ALL    // Registro toda la información posible: headers, body, etc.
        // Para producción, consideraría cambiarlo a LogLevel.INFO o LogLevel.NONE.
    }

    // Instalo y configuro el plugin HttpTimeout para controlar los tiempos de espera de las peticiones.
    // Esto evita que la aplicación se quede colgada indefinidamente si el servidor no responde.
    install(HttpTimeout) {
        // Tiempo máximo total para que una petición HTTP completa (incluyendo conexión, envío y recepción) finalice.
        requestTimeoutMillis = 30000 // 30 segundos
        // Tiempo máximo para establecer una conexión con el servidor.
        connectTimeoutMillis = 15000 // 15 segundos
        // Tiempo máximo de inactividad entre la llegada de dos paquetes de datos consecutivos una vez establecida la conexión.
        socketTimeoutMillis = 20000  // 20 segundos
    }
}