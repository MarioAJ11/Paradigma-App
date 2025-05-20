package com.example.paradigmaapp.android.api

import android.util.Log
import com.example.paradigmaapp.android.podcast.Podcast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

/**
 * [ArchiveService] es responsable de la comunicación con la API de archive.org
 * para obtener la información de los podcasts.
 *
 * Utiliza [io.ktor.client.HttpClient] de Ktor para realizar las peticiones de red de forma asíncrona.
 * Implementa una estrategia para obtener primero una lista básica de podcasts
 * y luego obtener los detalles de cada uno, utilizando una caché en memoria para optimizar.
 *
 * @author Mario Alguacil Juárez
 */
class ArchiveService { // Mantén la instancia del cliente aquí

    // Instancia única del [HttpClient] configurado para Android.
    private val client = HttpClient(Android) {
        expectSuccess = true
        // install(HttpTimeout) { // Opcional: Añadir timeouts para peticiones HEAD
        //     requestTimeoutMillis = 5000 // 5 segundos
        //     connectTimeoutMillis = 5000
        //     socketTimeoutMillis = 5000
        // }
    }

    internal companion object {
        // Define el número de resultados por página para las búsquedas en la API.
        const val PAGE_SIZE = 100

        // Define el tiempo de espera entre peticiones a la API para evitar sobrecargar el servidor.
        const val DELAY_BETWEEN_REQUESTS_MS = 300L

        // Caché en memoria para almacenar los objetos [Podcast] una vez que se han recuperado sus detalles.
        // La clave del mapa es el identificador único del podcast.
        // Modificado para almacenar información parcial inicialmente y actualizar con detalles.
        val podcastCache = mutableMapOf<String, Podcast>()
    }

    /**
     * Obtiene todos los podcasts para un [creator] específico.
     * Primero realiza una búsqueda para obtener los identificadores, título y duración básica de los podcasts,
     * y luego recupera los detalles completos (URL de audio, imagen y duración confirmada) de cada uno de forma asíncrona.
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
                            // Intentamos obtener de la caché.
                            val cachedPodcast = podcastCache[podcast.identifier]

                            // Si el podcast NO está en caché O le faltan detalles importantes (URL, Imagen, Duración real)
                            val shouldFetchDetails = cachedPodcast == null ||
                                    cachedPodcast.url.isEmpty() ||
                                    cachedPodcast.imageUrl.isNullOrEmpty() ||
                                    cachedPodcast.duration == "--:--" // Considerar que no tiene duración real

                            if (shouldFetchDetails) {
                                fetchPodcastDetails(podcast.identifier)?.also { detailedPodcast ->
                                    // Actualizamos la caché con los detalles completos.
                                    podcastCache[podcast.identifier] = detailedPodcast
                                } ?: (cachedPodcast // Si falla la obtención de detalles, usar la versión de la caché (si existe)
                                    ?: podcastCache.putIfAbsent(podcast.identifier, podcast) // O la básica de la búsqueda (y cachearla)
                                    ?: podcast) // O la versión de la búsqueda (si putIfAbsent devuelve null)
                            } else {
                                cachedPodcast // Usar la versión de la caché que ya tiene todos los detalles
                            }
                        }
                    }
                }
                val detailedPodcasts = detailRequests.awaitAll()

                // Solo añade los podcasts que obtuvimos (ya sean detallados o básicos si falló la descarga de detalles)
                allPodcasts.addAll(detailedPodcasts.filterNotNull())

                totalProcessed += podcastsFromPage.size

                if (totalProcessed < totalAvailable) {
                    page++
                    delay(DELAY_BETWEEN_REQUESTS_MS)
                }
            }
            Log.i("ArchiveService", "Descargados ${allPodcasts.size} podcasts con detalles (con caché)")
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error general al recuperar podcasts", e)
            // En caso de error, intentar devolver lo que haya en caché, si es que hay algo.
            if (allPodcasts.isEmpty() && podcastCache.isNotEmpty()) {
                Log.w("ArchiveService", "Devolviendo podcasts desde caché debido a un error.")
                return podcastCache.values.toList()
            }
        }
        return allPodcasts.distinctBy { it.identifier } // Asegurarse de que no haya duplicados si algo sale mal con la caché
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
        Log.d("ArchiveService", "Fetching page $page: $url")
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
                "&fl[]=identifier,title,duration" + // Solicitamos 'duration' en la búsqueda inicial.
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
        val json = Json.Default.parseToJsonElement(jsonResponse).jsonObject
        val response = json["response"]?.jsonObject ?: return Pair(emptyList(), 0)
        val totalResults = response["numFound"]?.toString()?.toIntOrNull() ?: 0
        val docs = response["docs"]?.jsonArray ?: return Pair(emptyList(), totalResults)

        val podcasts = docs.mapNotNull { element ->
            try {
                val jsonObject = element.jsonObject
                val identifier = jsonObject["identifier"]?.toString()?.trim('"') ?: ""
                val rawTitle = jsonObject["title"]?.toString()?.trim('"') ?: "Sin título"
                val rawDuration = jsonObject["duration"]?.toString()?.trim('"')

                // Creamos un objeto Podcast con la información inicial.
                // imageUrl y url serán vacías/null y se llenarán en fetchPodcastDetails.
                Podcast(
                    title = rawTitle,
                    url = "",
                    imageUrl = null, // Inicialmente null
                    duration = formatDuration(rawDuration), // Formateamos la duración de la búsqueda (puede ser imprecisa)
                    identifier = identifier
                )
            } catch (e: Exception) {
                Log.e("ArchiveService", "Error processing search result item: $e", e)
                null
            }
        }
        return Pair(podcasts, totalResults)
    }

    /**
     * Realiza una petición a la API de metadatos para obtener los detalles completos
     * de un podcast específico utilizando su [identifier].
     *
     * @param identifier El identificador único del podcast.
     * @return Un objeto [Podcast] con todos sus detalles (actualizado) o null si la petición falla o no se encuentra audio.
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
     * de un objeto [Podcast] existente (o crea uno si no está en caché)
     * con la URL del audio, la URL de la imagen de portada y la duración confirmada.
     *
     * @param jsonResponse El cuerpo de la respuesta JSON como [String].
     * @param identifier El identificador del podcast.
     * @return Un objeto [Podcast] actualizado con detalles o null si no se puede procesar (falta audio URL).
     */
    internal fun processMetadataResponse(jsonResponse: String, identifier: String): Podcast? {
        val json = Json.Default.parseToJsonElement(jsonResponse).jsonObject
        val metadata = json["metadata"]?.jsonObject
        val filesElement = json["files"]
        val files = if (filesElement is JsonElement) {
            filesElement.jsonArray
        } else {
            null
        }

        if (metadata == null) {
            Log.w("ArchiveService", "Campo 'metadata' nulo o no existe para $identifier")
            return null
        }

        val rawTitle = metadata["title"]?.toString()?.trim('"') ?: "Sin título"

        // *** CAMBIO CLAVE 1: Extraer duración del archivo de audio si está disponible ***
        var finalDuration = "--:--"
        val audioFileObject = files?.firstOrNull {
            val format = it.jsonObject["format"]?.toString()?.trim('"') ?: ""
            val name = it.jsonObject["name"]?.toString()?.trim('"') ?: ""
            format.contains("mp3", ignoreCase = true) ||
                    format.contains("ogg", ignoreCase = true) ||
                    format.contains("m4a", ignoreCase = true) ||
                    format.contains("aac", ignoreCase = true) ||
                    format.contains("wav", ignoreCase = true) ||
                    name.endsWith(".mp3", ignoreCase = true) ||
                    name.endsWith(".ogg", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true) ||
                    name.endsWith(".aac", ignoreCase = true) ||
                    name.endsWith(".wav", ignoreCase = true)
        }

        val audioDurationFromFiles = audioFileObject?.jsonObject?.get("length")?.toString()?.trim('"')
        if (!audioDurationFromFiles.isNullOrBlank()) {
            finalDuration = formatDuration(audioDurationFromFiles)
        } else {
            // Si no se encuentra en el archivo de audio, intentar con el metadata general (aunque dijiste que no estaba en tu JSON)
            val durationFromMetadata = metadata["length"]?.toString()?.trim('"') ?: metadata["duration"]?.toString()?.trim('"')
            if (!durationFromMetadata.isNullOrBlank()) {
                finalDuration = formatDuration(durationFromMetadata)
            }
        }
        // ********************************************************************************

        val audioUrl = findAudioUrlFromMetadata(files, identifier)
        if (audioUrl == null) {
            Log.w("ArchiveService", "No se encontró archivo de audio en metadatos para $identifier. No se puede crear un Podcast completo.")
            // Si no hay archivo de audio, no podemos reproducir, consideramos que no es un podcast válido.
            return null
        }

        // *** CAMBIO CLAVE 2: Pasar los 'files' a findCoverImage para una búsqueda más precisa ***
        val imageUrl = findCoverImage(identifier, files)

        // Crear una nueva instancia de Podcast con todos los detalles confirmados
        val detailedPodcast = Podcast(
            title = rawTitle,
            url = audioUrl,
            imageUrl = imageUrl,
            duration = finalDuration,
            identifier = identifier
        )
        // No necesitamos buscar en la caché aquí, simplemente creamos la versión completa
        // y la caché se actualizará en fetchAllPodcasts.
        return detailedPodcast
    }

    /**
     * Busca en la lista de archivos del metadato la URL del primer archivo de audio encontrado
     * con extensiones comunes como .mp3, .ogg o .m4a.
     *
     * @param files El [kotlinx.serialization.json.JsonArray] que contiene la información de los archivos.
     * @param identifier El identificador del item para construir la URL de descarga.
     * @return La URL del archivo de audio o null si no se encuentra ninguno.
     */
    private fun findAudioUrlFromMetadata(files: JsonArray?, identifier: String): String? {
        return files?.firstOrNull {
            val format = it.jsonObject["format"]?.toString()?.trim('"') ?: ""
            val name = it.jsonObject["name"]?.toString()?.trim('"') ?: ""
            // Añade más formatos si es necesario según lo que veas en la API
            format.contains("mp3", ignoreCase = true) ||
                    format.contains("ogg", ignoreCase = true) ||
                    format.contains("m4a", ignoreCase = true) ||
                    format.contains("aac", ignoreCase = true) ||
                    format.contains("wav", ignoreCase = true) ||
                    format.contains("audio", ignoreCase = true) || // Mantener por si acaso
                    format.contains("sound", ignoreCase = true) || // Mantener por si acaso
                    // Considerar también la extensión del nombre del archivo si 'format' no es explícito
                    name.endsWith(".mp3", ignoreCase = true) ||
                    name.endsWith(".ogg", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true) ||
                    name.endsWith(".aac", ignoreCase = true) ||
                    name.endsWith(".wav", ignoreCase = true)
        }?.jsonObject?.get("name")?.toString()?.trim('"')?.let { fileName ->
            "https://archive.org/download/$identifier/$fileName"
        }
    }

    /**
     * Intenta construir la URL de la imagen de portada del podcast utilizando los archivos disponibles
     * en la respuesta de metadatos y convenciones comunes de nombres de archivo en archive.org.
     *
     * @param identifier El identificador del item.
     * @param files El [kotlinx.serialization.json.JsonArray] que contiene la información de los archivos.
     * @return La URL de la imagen de portada o null si no se puede construir.
     */
    private fun findCoverImage(identifier: String, files: JsonArray?): String? {
        val baseUrl = "https://archive.org/download/$identifier"
        val possibleImageExtensions = listOf("jpg", "jpeg", "png", "gif") // Añadido jpeg por completitud

        // *** CAMBIO CLAVE 3: Priorizar la búsqueda de imágenes directamente en los 'files' ***
        // 1. Buscar por formatos conocidos de imagen
        files?.firstOrNull { file ->
            val format = file.jsonObject["format"]?.toString()?.trim('"') ?: ""
            possibleImageExtensions.any { ext -> format.contains(ext, ignoreCase = true) } ||
                    format.contains("thumbnail", ignoreCase = true) || // Incluir Item Tile / Item Image
                    format.contains("item tile", ignoreCase = true)
        }?.jsonObject?.get("name")?.toString()?.trim('"')?.let { fileName ->
            Log.d("ArchiveService", "Imagen encontrada por formato en files: $fileName")
            return "$baseUrl/${fileName.encodeUrl()}"
        }

        // 2. Si no se encontró por formato, buscar por nombres convencionales o derivados.
        // Esto es útil para imágenes que quizás no tienen un "format" explícito de imagen
        // o si queremos priorizar ciertos nombres.
        val audioFileNameWithoutExt = files?.firstOrNull {
            val name = it.jsonObject["name"]?.toString()?.trim('"') ?: ""
            name.endsWith(".mp3", ignoreCase = true) || name.endsWith(".ogg", ignoreCase = true) || name.endsWith(".m4a", ignoreCase = true)
        }?.jsonObject?.get("name")?.toString()?.trim('"')?.substringBeforeLast('.')

        val possibleBaseNames = mutableListOf(
            identifier,           // "item_identifier.jpg"
            "${identifier}_thumb",// "item_identifier_thumb.jpg"
            "cover",              // "cover.jpg"
            "thumb",              // "thumb.jpg"
            "album_art",          // "album_art.jpg"
            "folder",             // "folder.jpg"
            "logo",               // "logo.jpg"
            "image",              // "image.png"
            "__ia_thumb",         // Agregado específicamente para "__ia_thumb.jpg"
        )
        if (audioFileNameWithoutExt != null) {
            possibleBaseNames.add(0, audioFileNameWithoutExt) // Añadir al principio para probar primero
        }


        for (baseName in possibleBaseNames.distinct()) { // distinct() para evitar duplicados si audioFileNameWithoutExt coincide
            for (ext in possibleImageExtensions) {
                val potentialUrl = "$baseUrl/${baseName.encodeUrl()}.$ext"
                Log.d("ArchiveService", "Intentando URL de imagen: $potentialUrl")

                // Opcional: Podrías considerar una petición HEAD aquí para verificar si la imagen existe.
                // Sin embargo, para mantener el rendimiento y la simplicidad, a menudo se asume que
                // si la URL se construye correctamente, la imagen existe. Coil se encargará del fallo
                // si la URL es 404.
                return potentialUrl // Devolvemos la primera URL encontrada y construida.
            }
        }
        return null // Si ninguna combinación de nombres o formatos funciona.
    }

    /**
     * Formatea la duración del podcast de segundos (String o Float/Double) a un formato MM:SS o HH:MM:SS.
     * Maneja casos en los que la duración no es válida o está ausente.
     * Intenta parsear como Int, luego como Double si falla.
     *
     * @param rawDuration La duración como [String], puede ser un entero o un punto flotante.
     * @return La duración formateada como MM:SS o HH:MM:SS, o "--:--" si no es válida.
     */
    private fun formatDuration(rawDuration: String?): String {
        if (rawDuration.isNullOrBlank()) return "--:--"
        return try {
            val secondsFloat = rawDuration.toFloat()
            if (secondsFloat.isNaN() || secondsFloat.isInfinite() || secondsFloat < 0) return "--:--"
            val totalSeconds = secondsFloat.toLong()

            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val remainingSeconds = totalSeconds % 60

            if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
            } else {
                String.format("%02d:%02d", minutes, remainingSeconds)
            }
        } catch (e: NumberFormatException) {
            Log.w("ArchiveService", "Cannot parse duration '$rawDuration' as number: ${e.message}")
            "--:--"
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error formatting duration '$rawDuration': ${e.message}")
            "--:--"
        }
    }


    /**
     * Extensión de la clase [String] para codificar la URL de forma segura en UTF-8.
     * Utilizado para construir la query de búsqueda y URLs de archivos.
     *
     * @return La cadena codificada para ser utilizada en una URL.
     */
    private fun String.encodeUrl(): String {
        return URLEncoder.encode(this, "UTF-8")
    }
}