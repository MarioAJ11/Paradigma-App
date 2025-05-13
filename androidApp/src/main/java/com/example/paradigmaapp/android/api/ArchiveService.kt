package com.example.paradigmaapp.android.api

import android.util.Log
import com.example.paradigmaapp.android.podcast.Podcast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.head // Import HEAD request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode // Import HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
 */
class ArchiveService { // Mantén la instancia del cliente aquí

    // Instancia única del [HttpClient] configurado para Android.
    private val client = HttpClient(Android) {
        expectSuccess = true // Asegura que las respuestas HTTP con códigos de error lancen una excepción.
        // No necesitas configurar expectSuccess para la petición HEAD si vas a
        // manejar los códigos de estado manualmente.
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

        // URL del stream de Andaina (ajústala si es diferente)
        const val ANDAINA_STREAM_URL = "http://andaina.radios.cc:8000/andaina" // <-- Verifica esta URL
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

                // Primero, añadimos los podcasts de la búsqueda a la caché con la info básica.
                // Esto es útil si fetchPodcastDetails falla, tenemos al menos la info inicial.
                podcastsFromPage.forEach { podcast ->
                    podcastCache.putIfAbsent(podcast.identifier, podcast)
                }

                val detailRequests = withContext(Dispatchers.IO) {
                    podcastsFromPage.map { podcast ->
                        async {
                            // Intentamos obtener de la caché primero
                            podcastCache[podcast.identifier]?.let { cachedPodcast ->
                                // Si ya está en caché con una URL (indicando detalles completos), úsala.
                                if (cachedPodcast.url.isNotEmpty()) {
                                    return@async cachedPodcast
                                }
                            }

                            // Si no está en caché o no tiene URL, obtenemos los detalles.
                            fetchPodcastDetails(podcast.identifier)?.also { detailedPodcast ->
                                // Actualizamos la caché con los detalles completos.
                                podcastCache[podcast.identifier] = detailedPodcast
                            } ?: podcastCache[podcast.identifier] // Si falla, devuelve la versión básica de la caché.
                            ?: podcast // Si tampoco está en caché (raro), devuelve la versión de la búsqueda.
                        }
                    }
                }
                val detailedPodcasts = detailRequests.awaitAll()

                // Solo añade los podcasts que obtuvimos (ya sean detallados o básicos si falló la descarga de detalles)
                allPodcasts.addAll(detailedPodcasts.filterNotNull()) // Filter out any potential nulls if a request somehow returned null

                totalProcessed += podcastsFromPage.size

                if (totalProcessed < totalAvailable) {
                    page++
                    delay(DELAY_BETWEEN_REQUESTS_MS)
                }
            }
            Log.i("ArchiveService", "Descargados ${allPodcasts.size} podcasts con detalles (con caché)")
        } catch (e: Exception) {
            Log.e("ArchiveService", "Error general al recuperar podcasts", e)
            // En caso de error, intentar devolver lo que haya en caché.
            if (allPodcasts.isEmpty()) {
                return podcastCache.values.toList()
            }
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
                // Intentamos obtener la duración de la respuesta de búsqueda
                val rawDuration = jsonObject["duration"]?.toString()?.trim('"')

                // Creamos un objeto Podcast con la información inicial, incluyendo la duración si está disponible.
                Podcast(
                    title = rawTitle,
                    url = "", // La URL del audio se obtiene en los detalles.
                    imageUrl = null, // La URL de la imagen se obtiene en los detalles.
                    duration = formatDuration(rawDuration), // Formateamos la duración encontrada aquí.
                    identifier = identifier
                )
            } catch (e: Exception) {
                Log.e("ArchiveService", "Error processing search result item: $e", e)
                null // Ignora este elemento si falla su procesamiento.
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
            null // Devuelve null si falla la petición de metadatos.
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

        if (files == null) {
            Log.w("ArchiveService", "El campo 'files' no es un JsonArray o no existe para $identifier")
            // A veces los metadatos existen pero no hay archivos. Podemos intentar crear un podcast
            // básico si la caché ya tiene uno, aunque no tenga URL de audio.
            return podcastCache[identifier]?.copy(
                title = metadata["title"]?.toString()?.trim('"') ?: "Sin título",
                imageUrl = findCoverImage(identifier), // Intentar encontrar imagen aunque no haya audio
                // Mantenemos la duración que ya teníamos si existía.
                duration = podcastCache[identifier]?.duration ?: formatDuration(metadata["length"]?.toString()?.trim('"') ?: metadata["duration"]?.toString()?.trim('"'))
            ) // Si no está en caché, no podemos crear uno sin audio URL.
        }

        val rawTitle = metadata["title"]?.toString()?.trim('"') ?: "Sin título"
        // Preferimos la duración de 'length' o 'duration' en los metadatos si están presentes.
        // Si no, mantenemos la que pudimos obtener en la búsqueda.
        val durationFromMetadata = metadata["length"]?.toString()?.trim('"') ?: metadata["duration"]?.toString()?.trim('"')

        val audioUrl = findAudioUrlFromMetadata(files, identifier)
        if (audioUrl == null) {
            Log.w("ArchiveService", "No se encontró archivo de audio en metadatos para $identifier")
            // Si no hay archivo de audio, no podemos reproducir, consideramos que no es un podcast válido para detalles completos.
            // Podríamos devolver la versión básica de la caché si existe, pero null indica fallo en obtener detalles.
            return null
        }

        val imageUrl = findCoverImage(identifier)

        // Buscamos si ya existe una instancia en la caché para actualizarla,
        // o creamos una nueva si no (debería existir por la lógica de fetchAllPodcasts).
        return podcastCache[identifier]?.copy(
            title = rawTitle,
            url = audioUrl,
            imageUrl = imageUrl,
            // Usamos la duración de los metadatos si está disponible, de lo contrario, la de la búsqueda inicial.
            duration = formatDuration(durationFromMetadata ?: podcastCache[identifier]?.duration?.let { if (it == "--:--") null else it } )
        ) ?: Podcast( // Si no existe en la caché (raro), creamos una nueva instancia.
            title = rawTitle,
            url = audioUrl,
            imageUrl = imageUrl,
            duration = formatDuration(durationFromMetadata ?: ""), // Si no está en metadata, usamos vacío y formatDuration lo manejará
            identifier = identifier
        ).also { podcastCache[identifier] = it } // Asegurarnos de que la nueva instancia también se cachea.
    }

    /**
     * Busca en la lista de archivos del metadato la URL del primer archivo de audio encontrado
     * con extensiones comunes como .mp3, .ogg o .m4a.
     *
     * @param files El [kotlinx.serialization.json.JsonArray] que contiene la información de los archivos.
     * @param identifier El identificador del item para construir la URL de descarga.
     * @return La URL del archivo de audio o null si no se encuentra ninguno.
     */
    private fun findAudioUrlFromMetadata(files: JsonArray, identifier: String): String? {
        return files.firstOrNull {
            val format = it.jsonObject["format"]?.toString()?.trim('"') ?: ""
            format.contains("audio", ignoreCase = true) ||
                    format.contains("mp3", ignoreCase = true) ||
                    format.contains("ogg", ignoreCase = true) ||
                    format.contains("m4a", ignoreCase = true) ||
                    format.contains("sound", ignoreCase = true) // Añadir otros formatos comunes si es necesario
        }?.jsonObject?.get("name")?.toString()?.trim('"')?.let { fileName ->
            "https://archive.org/download/$identifier/$fileName"
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
        // Nombres comunes de imágenes de portada en archive.org
        val possibleCoverNames = listOf(
            "${identifier}_thumb.jpg", // Miniatura generada automáticamente
            "${identifier}.jpg",        // Imagen principal con el mismo nombre del item
            "cover.jpg",              // Nombre común de archivo de portada
            "thumb.jpg",              // Otro nombre común para miniaturas
            "album_art.jpg",          // Nombre común para arte de álbum
            "folder.jpg"              // Otro nombre común (menos probable para podcasts)
        )
        // Simplemente construimos las URLs. La verificación de si existen se haría
        // idealmente con una petición HEAD, pero por simplicidad, asumimos
        // que si una de estas URLs existe, es la imagen.
        return possibleCoverNames.firstOrNull { name ->
            // Podrías añadir lógica aquí para verificar la existencia de la imagen si es crucial,
            // pero generalmente, si el item tiene una portada, usa una de estas convenciones.
            true // Simplemente usa el primer nombre posible para construir la URL
        }?.let { coverName ->
            "$baseUrl/${coverName.encodeUrl()}" // Codificar el nombre del archivo por si acaso
        }
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
            val secondsFloat = rawDuration.toFloat() // Intenta como float primero por si acaso
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
            "--:--" // Valor por defecto en caso de error de formato.
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