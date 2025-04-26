package com.example.paradigmaapp.android

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

/**
 * [ArchiveService] es responsable de la comunicación con la API de archive.org
 * para obtener la información de los podcasts.
 *
 * Utiliza [HttpClient] de Ktor para realizar las peticiones de red de forma asíncrona.
 * Implementa una estrategia para obtener primero una lista básica de podcasts
 * y luego obtener los detalles de cada uno, utilizando una caché en memoria para optimizar.
 */
class ArchiveService {
    // Instancia única del [HttpClient] configurado para Android.
    private val client = HttpClient(Android) {
        expectSuccess = true // Asegura que las respuestas HTTP con códigos de error lancen una excepción.
    }

    internal companion object {
        // Define el número de resultados por página para las búsquedas en la API.
        const val PAGE_SIZE = 100

        // Define el tiempo de espera entre peticiones a la API para evitar sobrecargar el servidor.
        const val DELAY_BETWEEN_REQUESTS_MS = 300L

        // Caché en memoria para almacenar los objetos [Podcast] una vez que se han recuperado sus detalles.
        // La clave del mapa es el identificador único del podcast.
        val podcastCache = mutableMapOf<String, Podcast>()
    }

    /**
     * Obtiene todos los podcasts para un [creator] específico.
     * Primero realiza una búsqueda para obtener los identificadores y el título de los podcasts,
     * y luego recupera los detalles completos de cada uno de forma asíncrona.
     * Utiliza la caché para evitar peticiones repetidas.
     *
     * @param creator El nombre del creador de los podcasts a buscar (por defecto "mario011").
     * @return Una lista de [Podcast] con todos sus detalles.
     */
    suspend fun fetchAllPodcasts(creator: String = "mario011"): List<Podcast> {
        val allPodcasts = mutableListOf<Podcast>()
        var page = 1
        var totalProcessed = 0
        var totalAvailable = Int.MAX_VALUE

        try {
            while (totalProcessed < totalAvailable) {
                val response = fetchPage(creator, page)
                val (podcastsFromPage, total) = processSearchResponse(response)
                totalAvailable = total
                Log.d("ArchiveService", "🔍 Encontrados ${podcastsFromPage.size} podcasts en la página $page (Total: $totalAvailable)")

                val detailRequests = withContext(Dispatchers.IO) {
                    podcastsFromPage.map { podcast ->
                        async {
                            podcastCache[podcast.identifier] ?: fetchPodcastDetails(podcast.identifier)?.also {
                                podcastCache[podcast.identifier] = it
                            } ?: podcast // Si falla la obtención de detalles, usamos la información básica.
                        }
                    }
                }
                val detailedPodcasts = detailRequests.awaitAll()

                allPodcasts.addAll(detailedPodcasts)
                totalProcessed += podcastsFromPage.size

                if (totalProcessed < totalAvailable) {
                    page++
                    delay(DELAY_BETWEEN_REQUESTS_MS)
                }
            }
            Log.i("ArchiveService", "Descargados ${allPodcasts.size} podcasts con detalles (con caché)")
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error general al recuperar podcasts", e)
        }
        return allPodcasts
    }

    /**
     * Realiza una petición GET a la API de búsqueda de archive.org para obtener una página de resultados.
     *
     * @param creator El nombre del creador a buscar.
     * @param page El número de página de resultados a obtener.
     * @return El cuerpo de la respuesta JSON como un [String].
     */
    internal suspend fun fetchPage(creator: String, page: Int): String {
        val url = buildSearchUrl(creator, page)
        Log.d("ArchiveService", "Fetching page $page...")
        return client.get(url).bodyAsText()
    }

    /**
     * Construye la URL para la API de búsqueda avanzada de archive.org.
     * Solicita los campos 'identifier', 'title' y 'duration' para la lista inicial de podcasts.
     *
     * @param creator El nombre del creador.
     * @param page El número de página.
     * @return La URL completa para la petición de búsqueda.
     */
    private fun buildSearchUrl(creator: String, page: Int): String {
        return "https://archive.org/advancedsearch.php?" +
                "q=${"creator:\"$creator\" AND mediatype:audio".encodeUrl()}" +
                "&fl[]=identifier,title,duration" + // Solicitamos también la duración en la búsqueda inicial.
                "&rows=$PAGE_SIZE" +
                "&page=$page" +
                "&sort[]=date+desc" + // Ordena los resultados por fecha descendente.
                "&output=json"
    }

    /**
     * Procesa la respuesta JSON de la API de búsqueda y extrae una lista de objetos [Podcast]
     * con la información básica (identifier, title, duration) y el número total de resultados.
     *
     * @param jsonResponse El cuerpo de la respuesta JSON como [String].
     * @return Un [Pair] que contiene la lista de [Podcast] y el número total de resultados.
     */
    internal fun processSearchResponse(jsonResponse: String): Pair<List<Podcast>, Int> {
        val json = Json.parseToJsonElement(jsonResponse).jsonObject
        val response = json["response"]?.jsonObject ?: return Pair(emptyList(), 0)
        val totalResults = response["numFound"]?.toString()?.toIntOrNull() ?: 0
        val docs = response["docs"]?.jsonArray ?: return Pair(emptyList(), totalResults)
        val podcasts = docs.mapNotNull { element ->
            val jsonObject = element.jsonObject
            val identifier = jsonObject["identifier"]?.toString()?.trim('"') ?: ""
            val titleWithDuration = jsonObject["title"]?.toString()?.trim('"') ?: "Sin título" // Usamos el título tal cual viene inicialmente
            val duration = jsonObject["duration"]?.toString()?.trim('"')
            Podcast(
                title = titleWithDuration, // El título con la duración (si está presente inicialmente)
                url = "",
                imageUrl = null,
                duration = formatDuration(duration),
                identifier = identifier // Asegúrate de que el identifier se esté pasando si lo añadiste a la clase Podcast
            )
        }
        return Pair(podcasts, totalResults)
    }

    /**
     * Realiza una petición a la API de metadatos para obtener los detalles completos
     * de un podcast específico utilizando su [identifier].
     *
     * @param identifier El identificador único del podcast.
     * @return Un objeto [Podcast] con todos sus detalles, o null si la petición falla.
     */
    private suspend fun fetchPodcastDetails(identifier: String): Podcast? {
        val url = "https://archive.org/metadata/$identifier"
        Log.d("ArchiveService", "Fetching details for $identifier...")
        return try {
            val response = client.get(url).body<String>()
            processMetadataResponse(response, identifier)
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error al obtener metadatos de $identifier", e)
            null
        }
    }

    /**
     * Procesa la respuesta JSON de la API de metadatos y actualiza la información
     * de un objeto [Podcast] existente con la URL del audio y la URL de la imagen de portada.
     * También intenta obtener la duración de los metadatos si no se obtuvo en la búsqueda inicial.
     *
     * @param jsonResponse El cuerpo de la respuesta JSON como [String].
     * @param identifier El identificador del podcast.
     * @return Un objeto [Podcast] actualizado o null si no se puede procesar.
     */
    private fun processMetadataResponse(jsonResponse: String, identifier: String): Podcast? {
        val json = Json.parseToJsonElement(jsonResponse).jsonObject
        val metadata = json["metadata"]?.jsonObject ?: return null
        val filesElement = json["files"]
        val files = if (filesElement is JsonElement) {
            filesElement.jsonArray
        } else {
            null
        }

        if (files == null) {
            Log.w("ArchiveService", "El campo 'files' no es un JsonArray o no existe para $identifier")
            return null
        }

        val rawTitle = metadata["title"]?.toString()?.trim('"') ?: "Sin título"
        // Intentamos obtener la duración de los metadatos.
        var rawDuration = metadata["length"]?.toString()?.trim('"')

        // Si la duración sigue siendo nula o vacía después de la búsqueda inicial, la obtenemos de los metadatos.
        if (rawDuration.isNullOrEmpty()) {
            rawDuration = metadata["duration"]?.toString()?.trim('"')
        }

        val audioUrl = findAudioUrlFromMetadata(files, identifier)
        if (audioUrl == null) {
            Log.w("ArchiveService", "No se encontró archivo de audio en metadatos para $identifier")
            return null
        }
        val duration = formatDuration(rawDuration)
        val imageUrl = findCoverImage(identifier)

        // Buscamos si ya existe una instancia en la caché y la actualizamos.
        return podcastCache[identifier]?.copy(
            title = rawTitle,
            url = audioUrl,
            imageUrl = imageUrl,
            duration = duration
        ) ?: Podcast( // Si no existe en la caché (raro), creamos una nueva instancia.
            title = rawTitle,
            url = audioUrl,
            imageUrl = imageUrl,
            duration = duration,
            identifier = identifier
        ).also { podcastCache[identifier] = it }
    }

    /**
     * Busca en la lista de archivos del metadato la URL del primer archivo de audio encontrado
     * con extensiones comunes como .mp3, .ogg o .m4a.
     *
     * @param files El [JsonArray] que contiene la información de los archivos.
     * @param identifier El identificador del item para construir la URL de descarga.
     * @return La URL del archivo de audio o null si no se encuentra ninguno.
     */
    private fun findAudioUrlFromMetadata(files: JsonArray, identifier: String): String? {
        return files.firstOrNull {
            val name = it.jsonObject["name"]?.toString()?.trim('"') ?: ""
            name.endsWith(".mp3", ignoreCase = true) ||
                    name.endsWith(".ogg", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true)
        }?.jsonObject?.get("name")?.toString()?.trim('"')?.let {
            "https://archive.org/download/$identifier/$it"
        }
    }

    /**
     * Intenta construir la URL de la imagen de portada del podcast utilizando convenciones comunes
     * de nombres de archivo en archive.org.
     *
     * @param identifier El identificador del item.
     * @return La URL de la imagen de portada o null si no se puede construir.
     */
    private fun findCoverImage(identifier: String): String? {
        val baseUrl = "https://archive.org/download/$identifier"
        val possibleCovers = listOf(
            "${identifier}_thumb.jpg",
            "${identifier}.jpg",
            "cover.jpg",
            "thumb.jpg"
        )
        return possibleCovers.firstOrNull()?.let { "$baseUrl/$it" }
    }

    /**
     * Formatea la duración del podcast de segundos (String) a un formato MM:SS.
     * Maneja casos en los que la duración no es válida o está ausente.
     *
     * @param rawDuration La duración en segundos como [String].
     * @return La duración formateada como MM:SS o "--:--" si no es válida.
     */
    private fun formatDuration(rawDuration: String?): String {
        if (rawDuration.isNullOrBlank()) return "--:--"
        return try {
            val seconds = rawDuration.toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            String.format("%02d:%02d", minutes, remainingSeconds)
        } catch (e: NumberFormatException) {
            "--:--" // Valor por defecto en caso de error de formato.
        }
    }

    /**
     * Extensión de la clase [String] para codificar la URL de forma segura en UTF-8.
     * Utilizado para construir la query de búsqueda.
     *
     * @return La cadena codificada para ser utilizada en una URL.
     */
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}