package com.example.paradigmaapp.config

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.cache.Database
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Servicio para gestionar la configuración remota de la aplicación.
 * Descarga una configuración JSON, la guarda en caché y la proporciona al resto de la app.
 *
 * @param database La base de datos para guardar en caché la última configuración válida.
 */
class RemoteConfigService(private val database: Database) {

    // --- ¡IMPORTANTE! Reemplaza esta URL por la URL "Raw" de tu fichero en GitHub ---
    private val configUrl = "https://raw.githubusercontent.com/tu-usuario/tu-repositorio/main/app_config.json"

    private var inMemoryConfig: AppConfig? = null

    fun getConfig(): AppConfig {
        return inMemoryConfig ?: loadConfigFromCache() ?: getDefaultConfig()
    }

    suspend fun fetchAndCacheConfig() {
        try {
            val remoteConfig = ktorClient.get(configUrl).body<AppConfig>()
            inMemoryConfig = remoteConfig
            saveConfigToCache(remoteConfig)
        } catch (e: Exception) {
            println("Error al descargar la configuración remota: ${e.message}")
        }
    }

    private fun saveConfigToCache(config: AppConfig) {
        val configJson = Json.encodeToString(config)
        database.keyValueStoreQueries.insertValue("app_config", configJson)
    }

    /**
     * Carga la configuración desde la caché de la base de datos.
     */
    private fun loadConfigFromCache(): AppConfig? {
        return try {
            // La consulta ahora devuelve un String? directamente, por lo que ya no necesitamos ".value".
            val configJson = database.keyValueStoreQueries.getValue("app_config").executeAsOneOrNull()

            // Si el JSON no es nulo, lo decodificamos.
            configJson?.let { Json.decodeFromString<AppConfig>(it) }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun getDefaultConfig() = AppConfig(
            wordpressApiBaseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2",
            mainWebsiteUrl = "https://paradigmamedia.org/",
            liveStreamApiUrl = "https://radio.andaina.net/",
            liveStreamUrl = "https://radio.andaina.net/8042/stream"
        )
    }
}