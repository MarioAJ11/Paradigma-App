package com.example.paradigmaapp.android

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Servicio avanzado para recuperar podcasts de Archive.org con:
 * - Paginación completa (sin límite de resultados)
 * - Duración de cada podcast
 * - Portadas/thumbnails
 * - URLs de audio válidas
 */
class ArchiveService {
    private val client = HttpClient(Android) {
        expectSuccess = true
    }

    // Tamaño de página grande para minimizar requests (máximo permitido por la API)
    private companion object {
        const val PAGE_SIZE = 100
        const val DELAY_BETWEEN_REQUESTS_MS = 300L
    }

    /**
     * Recupera TODOS los podcasts de un creador con metadatos completos
     * @param creator Nombre del creador (por defecto "mario011")
     * @return Lista completa de Podcasts encontrados
     */
    suspend fun fetchAllPodcasts(creator: String = "mario011"): List<Podcast> {
        val allPodcasts = mutableListOf<Podcast>()
        var page = 1
        var totalProcessed = 0
        var totalAvailable = Int.MAX_VALUE

        try {
            while (totalProcessed < totalAvailable) {
                val response = fetchPage(creator, page)
                val (podcasts, total) = processResponse(response)

                allPodcasts.addAll(podcasts)
                totalAvailable = total
                totalProcessed += podcasts.size

                if (totalProcessed < totalAvailable) {
                    page++
                    delay(DELAY_BETWEEN_REQUESTS_MS)
                }
            }
            Log.i("ArchiveService", "✅ Descargados ${allPodcasts.size} podcasts")
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error recuperando podcasts", e)
        }

        return allPodcasts
    }

    /**
     * Recupera una página específica de resultados
     * @param creator Nombre del creador
     * @param page Número de página a recuperar
     * @return Respuesta JSON como String
     */
    private suspend fun fetchPage(creator: String, page: Int): String {
        val url = buildUrl(creator, page)
        Log.d("ArchiveService", "📡 Fetching page $page...")
        return client.get(url).bodyAsText()
    }

    /**
     * Construye URL de búsqueda con parámetros óptimos
     * @param creator Nombre del creador
     * @param page Número de página
     * @return URL completa para la consulta
     */
    private fun buildUrl(creator: String, page: Int): String {
        return "https://archive.org/advancedsearch.php?" +
                "q=${"creator:\"$creator\" AND mediatype:audio".encodeUrl()}" +
                "&fl[]=identifier,title,length,files" +
                "&rows=$PAGE_SIZE" +
                "&page=$page" +
                "&sort[]=date+desc" +  // Más recientes primero
                "&output=json"
    }

    /**
     * Procesa la respuesta JSON y extrae los podcasts
     * @param jsonResponse Respuesta JSON como String
     * @return Pair con lista de podcasts y total de resultados
     */
    private fun processResponse(jsonResponse: String): Pair<List<Podcast>, Int> {
        val json = Json.parseToJsonElement(jsonResponse).jsonObject
        val response = json["response"]?.jsonObject ?: return Pair(emptyList(), 0)

        val totalResults = response["numFound"]?.toString()?.toIntOrNull() ?: 0
        val docs = response["docs"]?.jsonArray ?: return Pair(emptyList(), totalResults)

        val podcasts = docs.mapNotNull { element ->
            try {
                element.jsonObject?.let { createPodcast(it) }
            } catch (e: Exception) {
                Log.e("ArchiveService", "Error procesando documento", e)
                null
            }
        }

        return Pair(podcasts, totalResults)
    }

    /**
     * Crea un objeto Podcast a partir de un documento JSON
     * @param doc Objeto JSON con los datos del podcast
     * @return Objeto Podcast o null si no es válido
     */
    private fun createPodcast(doc: JsonObject): Podcast? {
        val identifier = doc["identifier"]?.toString()?.trim('"') ?: return null
        val rawTitle = doc["title"]?.toString()?.trim('"') ?: "Sin título"
        val duration = formatDuration(doc["length"]?.toString()?.trim('"'))

        // Busca archivo de audio válido
        val audioUrl = findAudioUrl(doc["files"]?.jsonObject, identifier)
        if (audioUrl == null) {
            Log.w("ArchiveService", "⚠️ No se encontró archivo de audio para $identifier")
            return null
        }

        return Podcast(
            title = "$rawTitle • $duration",
            url = audioUrl,
            imageUrl = findCoverImage(identifier),
            duration = duration
        )
    }

    /**
     * Busca URL de audio válida
     * @param files Objeto JSON con los archivos
     * @param identifier Identificador del item
     * @return URL de audio o null si no se encuentra
     */
    private fun findAudioUrl(files: JsonObject?, identifier: String): String? {
        files?.entries?.forEach { (key, _) ->
            if (key.endsWith(".mp3", ignoreCase = true) ||
                key.endsWith(".ogg", ignoreCase = true) ||
                key.endsWith(".m4a", ignoreCase = true)) {
                return "https://archive.org/download/$identifier/$key"
            }
        }
        return null
    }

    /**
     * Busca imagen de portada
     * @param identifier Identificador del item
     * @return URL de la imagen o null si no se encuentra
     */
    private fun findCoverImage(identifier: String): String? {
        val baseUrl = "https://archive.org/download/$identifier"
        val possibleCovers = listOf(
            "${identifier}_thumb.jpg",  // Thumbnail estándar
            "${identifier}.jpg",        // Nombre directo
            "cover.jpg",                // Archivo genérico
            "thumb.jpg"                // Alternativa común
        )

        return possibleCovers.firstOrNull()?.let { "$baseUrl/$it" }
    }

    /**
     * Formatea la duración en segundos a MM:SS
     * @param rawDuration Duración en segundos como String
     * @return String formateado como MM:SS o "--:--" si no es válido
     */
    private fun formatDuration(rawDuration: String?): String {
        if (rawDuration.isNullOrBlank()) return "--:--"

        return try {
            val seconds = rawDuration.toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            String.format("%02d:%02d", minutes, remainingSeconds)
        } catch (e: NumberFormatException) {
            "--:--"
        }
    }

    /**
     * Codifica string para URL
     * @return String codificado en UTF-8
     */
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}