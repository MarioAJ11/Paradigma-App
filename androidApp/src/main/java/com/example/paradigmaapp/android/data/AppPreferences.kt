package com.example.paradigmaapp.android.data

import android.content.Context
import android.content.SharedPreferences
import com.example.paradigmaapp.model.Episodio
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gestiona el almacenamiento y la recuperación de las preferencias de la aplicación
 * utilizando [SharedPreferences]. Realiza la serialización y deserialización de
 * objetos complejos (como listas y mapas) a formato JSON para su almacenamiento.
 *
 * @param context El [Context] de la aplicación, necesario para acceder a SharedPreferences.
 * @author Mario Alguacil Juárez
 */
class AppPreferences(context: Context) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ParadigmaAppPrefsV2"
        private const val PREF_CURRENT_EPISODE_ID = "currentEpisodeId_v2"
        private const val PREF_IS_STREAM_ACTIVE = "isStreamActive_v2"
        private const val PREF_EPISODE_POSITIONS = "episodePositions_v2"
        private const val PREF_EPISODE_QUEUE_IDS = "episodeQueueIds_v2"
        private const val PREF_DOWNLOADED_EPISODIOS = "downloadedEpisodios_v1"
        private const val PREF_EPISODE_DETAILS_MAP = "episodeDetailsMap_v1"
        private const val PREF_ONBOARDING_COMPLETE = "onboardingComplete_v1"
        private const val PREF_MANUALLY_SET_DARK_THEME = "manuallySetDarkTheme_v1"
    }

    /**
     * Guarda si el usuario ha completado la pantalla de introducción.
     */
    fun saveOnboardingComplete(isComplete: Boolean) {
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, isComplete).apply()
    }

    /**
     * Comprueba si el usuario ha completado la pantalla de introducción.
     * @return `true` si ya la ha completado, `false` si es la primera vez.
     */
    fun loadOnboardingComplete(): Boolean {
        return prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)
    }

    /**
     * Guarda la posición de reproducción de un episodio.
     *
     * @param episodeId El ID del episodio.
     * @param positionMillis La posición de reproducción en milisegundos.
     */
    fun saveEpisodePosition(episodeId: Int, positionMillis: Long) {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap: MutableMap<String, Long> = try {
            // Decodificar a un mapa mutable. Si el JSON está vacío, se creará uno nuevo.
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()),
                positionsJson
            ).toMutableMap() // Crear una copia mutable segura
        } catch (e: SerializationException) {
            mutableMapOf() // En caso de error, empezar con un mapa nuevo.
        }
        positionsMap[episodeId.toString()] = positionMillis
        val newPositionsJson = jsonParser.encodeToString(positionsMap)
        prefs.edit().putString(PREF_EPISODE_POSITIONS, newPositionsJson).apply()
    }

    /**
     * Obtiene la posición de reproducción guardada para un episodio.
     *
     * @param episodeId El ID del episodio.
     * @return La posición en milisegundos, o 0L si no se encuentra.
     */
    fun getEpisodePosition(episodeId: Int): Long {
        val positionsMap = getAllEpisodePositions()
        return positionsMap[episodeId.toString()] ?: 0L
    }

    /**
     * Obtiene el mapa completo de todas las posiciones de episodios guardadas.
     *
     * @return Un mapa de ID de episodio (String) a su posición (Long).
     */
    fun getAllEpisodePositions(): Map<String, Long> {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        return try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()),
                positionsJson
            )
        } catch (e: SerializationException) {
            emptyMap() // Devolver mapa vacío si el JSON es inválido
        }
    }

    /** Guarda el ID del episodio actualmente activo.
     *
     * @param episodeId El ID del episodio o `null` si no se encuentra.
     */
    fun saveCurrentEpisodeId(episodeId: Int?) {
        val editor = prefs.edit()
        if (episodeId == null) {
            editor.remove(PREF_CURRENT_EPISODE_ID)
        } else {
            editor.putInt(PREF_CURRENT_EPISODE_ID, episodeId)
        }
        editor.apply()
    }

    /** Carga el ID del episodio actualmente activo.
     *
     * @return El ID del episodio o `null` si no se encuentra.
     */
    fun loadCurrentEpisodeId(): Int? {
        return if (prefs.contains(PREF_CURRENT_EPISODE_ID)) {
            prefs.getInt(PREF_CURRENT_EPISODE_ID, -1).takeIf { it != -1 }
        } else {
            null
        }
    }

    /** Guarda la preferencia de si el streaming debe estar activo al iniciar.
     *
     * @param isActive `true` si el streaming debe estar activo al iniciar, `false` en caso contrario.
     */
    fun saveIsStreamActive(isActive: Boolean) {
        prefs.edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    }

    /** Carga la preferencia de si el streaming debe estar activo.
     *
     * @return `true` si el streaming debe estar activo al iniciar, `false` en caso contrario.
     */
    fun loadIsStreamActive(): Boolean {
        return prefs.getBoolean(PREF_IS_STREAM_ACTIVE, true) //
    }

    /** Guarda la cola de reproducción (lista de IDs).
     *
     * @param queueEpisodeIds Una lista de enteros que representan los IDs de los episodios en la cola.
     */
    fun saveEpisodeQueue(queueEpisodeIds: List<Int>) {
        val jsonString = jsonParser.encodeToString(ListSerializer(Int.serializer()), queueEpisodeIds)
        prefs.edit().putString(PREF_EPISODE_QUEUE_IDS, jsonString).apply()
    }

    /** Carga la cola de reproducción (lista de IDs).
     *
     *  @return Una lista de enteros que representan los IDs de los episodios en la cola.
     */
    fun loadEpisodeQueue(): List<Int> {
        val jsonString = prefs.getString(PREF_EPISODE_QUEUE_IDS, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                jsonParser.decodeFromString(ListSerializer(Int.serializer()), jsonString)
            } catch (e: SerializationException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /** Guarda la lista completa de objetos Episodio que han sido descargados.
     *
     *  @param downloadedEpisodios La lista de objetos Episodio a guardar.
     */
    fun saveDownloadedEpisodios(downloadedEpisodios: List<Episodio>) {
        // Para serializar una lista de objetos complejos, necesitamos el serializador del objeto.
        val serializer: KSerializer<List<Episodio>> = ListSerializer(Episodio.serializer())
        val jsonString = jsonParser.encodeToString(serializer, downloadedEpisodios)
        prefs.edit().putString(PREF_DOWNLOADED_EPISODIOS, jsonString).apply()
    }

    /** Carga la lista de objetos Episodio que están descargados.
     *
     * @return Una lista de objetos Episodio. Si no hay datos, devuelve una lista vacía.
     */
    fun loadDownloadedEpisodios(): List<Episodio> {
        val jsonString = prefs.getString(PREF_DOWNLOADED_EPISODIOS, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                val serializer: KSerializer<List<Episodio>> = ListSerializer(Episodio.serializer())
                jsonParser.decodeFromString(serializer, jsonString)
            } catch (e: SerializationException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /** Guarda los detalles completos de un episodio en un mapa persistido.
     *
     * @param episodio El episodio a guardar.
     */
    fun saveEpisodioDetails(episodio: Episodio) {
        val detailsJson = prefs.getString(PREF_EPISODE_DETAILS_MAP, "{}") ?: "{}"
        val detailsMap: MutableMap<String, String> = try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                detailsJson
            ).toMutableMap()
        } catch (e: SerializationException) {
            mutableMapOf()
        }
        detailsMap[episodio.id.toString()] = jsonParser.encodeToString(episodio)
        val newDetailsJson = jsonParser.encodeToString(detailsMap)
        prefs.edit().putString(PREF_EPISODE_DETAILS_MAP, newDetailsJson).apply()
    }

    /** Carga los detalles de un episodio específico desde el mapa persistido.
     *
     * @param episodioId El ID del episodio a cargar.
     * @return Los detalles del episodio o `null` si no se encuentra.
     */
    fun loadEpisodioDetails(episodioId: Int): Episodio? {
        val detailsJson = prefs.getString(PREF_EPISODE_DETAILS_MAP, "{}") ?: "{}"
        val detailsMap: Map<String, String> = try {
            jsonParser.decodeFromString(MapSerializer(String.serializer(), String.serializer()), detailsJson)
        } catch (e: SerializationException) {
            return null
        }
        val episodioJson = detailsMap[episodioId.toString()]
        return if (episodioJson != null) {
            try {
                jsonParser.decodeFromString<Episodio>(episodioJson)
            } catch (e: SerializationException) {
                null
            }
        } else {
            null
        }
    }

    /** Guarda la preferencia manual del usuario para el tema.
     *
     *  @param isDark `true` si el tema está configurado como oscuro, `false` si está configurado como claro, o `null` si no se encuentra.
     */
    fun saveIsManuallySetDarkTheme(isDark: Boolean?) {
        val editor = prefs.edit()
        if (isDark == null) {
            editor.remove(PREF_MANUALLY_SET_DARK_THEME)
        } else {
            editor.putBoolean(PREF_MANUALLY_SET_DARK_THEME, isDark)
        }
        editor.apply()
    }

    /** Carga la preferencia manual del tema.
     *
     * @return `true` si el tema está configurado como oscuro, `false` si está configurado como claro, o `null` si no se encuentra.
     */
    fun loadIsManuallySetDarkTheme(): Boolean? {
        return if (prefs.contains(PREF_MANUALLY_SET_DARK_THEME)) {
            prefs.getBoolean(PREF_MANUALLY_SET_DARK_THEME, false)
        } else {
            null
        }
    }
}