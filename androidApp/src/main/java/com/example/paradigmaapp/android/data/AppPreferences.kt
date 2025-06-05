package com.example.paradigmaapp.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer // Para listas
import kotlinx.serialization.builtins.MapSerializer // Para mapas
import kotlinx.serialization.builtins.serializer // Para tipos básicos como String, Long, Int

/**
 * Gestiona el almacenamiento y la recuperación de las preferencias de la aplicación
 * utilizando [SharedPreferences]. Realiza la serialización y deserialización de
 * objetos complejos (como listas y mapas) a formato JSON para su almacenamiento.
 *
 * @param context El [Context] de la aplicación, necesario para acceder a SharedPreferences.
 * @property jsonParser Instancia de [Json] para la serialización/deserialización, configurada
 * para ser leniente y omitir claves desconocidas.
 * @property prefs Instancia de [SharedPreferences] para el almacenamiento de datos.
 *
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
        private const val PREFS_NAME = "ParadigmaAppPrefsV2" //
        private const val PREF_CURRENT_EPISODE_ID = "currentEpisodeId_v2" //
        private const val PREF_IS_STREAM_ACTIVE = "isStreamActive_v2" //
        private const val PREF_EPISODE_POSITIONS = "episodePositions_v2" //
        private const val PREF_EPISODE_QUEUE_IDS = "episodeQueueIds_v2" //
        private const val PREF_DOWNLOADED_EPISODE_IDS = "downloadedEpisodeIds_v2" //
        private const val PREF_MANUALLY_SET_DARK_THEME = "manuallySetDarkTheme_v1" // Clave para la preferencia de tema manual
    }

    /**
     * Guarda la posición de reproducción de un episodio.
     * Las posiciones se almacenan en un mapa serializado a JSON.
     *
     * @param episodeId El ID del episodio.
     * @param positionMillis La posición de reproducción en milisegundos.
     */
    fun saveEpisodePosition(episodeId: Int, positionMillis: Long) {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap: MutableMap<String, Long> = try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()), // Serializador explícito para el mapa
                positionsJson
            )
        } catch (e: Exception) {
            // En caso de error al decodificar, se inicia con un mapa vacío.
            // Considerar loggear este error en un sistema de monitoreo para producción si es frecuente.
            mutableMapOf()
        } as MutableMap<String, Long>
        positionsMap[episodeId.toString()] = positionMillis
        prefs.edit().putString(PREF_EPISODE_POSITIONS, jsonParser.encodeToString(positionsMap)).apply()
    }

    /**
     * Obtiene la posición de reproducción guardada para un episodio.
     *
     * @param episodeId El ID del episodio.
     * @return La posición de reproducción en milisegundos. Devuelve 0L si no hay posición guardada.
     */
    fun getEpisodePosition(episodeId: Int): Long {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap: Map<String, Long> = try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()),
                positionsJson
            )
        } catch (e: Exception) {
            // En caso de error, devuelve un mapa vacío.
            emptyMap()
        }
        return positionsMap[episodeId.toString()] ?: 0L
    }

    /**
     * Guarda el ID del episodio actualmente en reproducción o seleccionado.
     *
     * @param episodeId El ID del episodio. Si es null, se elimina la preferencia.
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

    /**
     * Carga el ID del episodio actualmente en reproducción o seleccionado.
     *
     * @return El ID del episodio, o null si no hay ninguno guardado.
     */
    fun loadCurrentEpisodeId(): Int? {
        return if (prefs.contains(PREF_CURRENT_EPISODE_ID)) {
            prefs.getInt(PREF_CURRENT_EPISODE_ID, -1).takeIf { it != -1 }
        } else {
            null
        }
    }

    /**
     * Guarda la preferencia del usuario sobre si el streaming de Andaina FM debe estar activo por defecto.
     *
     * @param isActive `true` si el streaming debe estar activo, `false` en caso contrario.
     */
    fun saveIsStreamActive(isActive: Boolean) {
        prefs.edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    }

    /**
     * Carga la preferencia del usuario sobre si el streaming de Andaina FM debe estar activo por defecto.
     *
     * @return `true` si el streaming debe estar activo (o si no hay preferencia guardada, por defecto es true),
     * `false` en caso contrario.
     */
    fun loadIsStreamActive(): Boolean {
        return prefs.getBoolean(PREF_IS_STREAM_ACTIVE, true) //
    }

    /**
     * Guarda la cola de reproducción (lista de IDs de episodios).
     * La lista se serializa a JSON.
     *
     * @param queueEpisodeIds La lista de IDs de episodios en la cola.
     */
    fun saveEpisodeQueue(queueEpisodeIds: List<Int>) {
        val jsonString = jsonParser.encodeToString(ListSerializer(Int.serializer()), queueEpisodeIds)
        prefs.edit().putString(PREF_EPISODE_QUEUE_IDS, jsonString).apply()
    }

    /**
     * Carga la cola de reproducción (lista de IDs de episodios).
     * Deserializa la lista desde JSON.
     *
     * @return La lista de IDs de episodios en la cola. Devuelve una lista vacía si no hay cola guardada o hay error.
     */
    fun loadEpisodeQueue(): List<Int> {
        val jsonString = prefs.getString(PREF_EPISODE_QUEUE_IDS, null)
        return if (jsonString != null) {
            try {
                jsonParser.decodeFromString(ListSerializer(Int.serializer()), jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Guarda la lista de IDs de episodios descargados.
     * La lista se serializa a JSON.
     *
     * @param downloadedEpisodeIds La lista de IDs de episodios descargados.
     */
    fun saveDownloadedEpisodeIds(downloadedEpisodeIds: List<Int>) {
        val jsonString = jsonParser.encodeToString(ListSerializer(Int.serializer()), downloadedEpisodeIds)
        prefs.edit().putString(PREF_DOWNLOADED_EPISODE_IDS, jsonString).apply()
    }

    /**
     * Carga la lista de IDs de episodios descargados.
     * Deserializa la lista desde JSON.
     *
     * @return La lista de IDs de episodios descargados. Devuelve una lista vacía si no hay datos guardados o hay error.
     */
    fun loadDownloadedEpisodeIds(): List<Int> {
        val jsonString = prefs.getString(PREF_DOWNLOADED_EPISODE_IDS, null)
        return if (jsonString != null) {
            try {
                jsonParser.decodeFromString(ListSerializer(Int.serializer()), jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Guarda la preferencia manual del usuario para el tema de la aplicación (claro/oscuro/sistema).
     *
     * @param isDark `true` si el usuario seleccionó tema oscuro, `false` para tema claro,
     * `null` si el usuario quiere seguir la configuración del sistema.
     */
    fun saveIsManuallySetDarkTheme(isDark: Boolean?) {
        val editor = prefs.edit()
        if (isDark == null) {
            editor.remove(PREF_MANUALLY_SET_DARK_THEME) // null significa seguir al sistema
        } else {
            editor.putBoolean(PREF_MANUALLY_SET_DARK_THEME, isDark)
        }
        editor.apply()
    }

    /**
     * Carga la preferencia manual del tema de la aplicación.
     *
     * @return `true` si el tema oscuro fue seleccionado manualmente, `false` si fue el claro,
     * o `null` si el usuario no ha establecido una preferencia manual (debe seguir al sistema).
     */
    fun loadIsManuallySetDarkTheme(): Boolean? {
        return if (prefs.contains(PREF_MANUALLY_SET_DARK_THEME)) {
            // El valor por defecto aquí no importa mucho si la clave existe.
            prefs.getBoolean(PREF_MANUALLY_SET_DARK_THEME, false)
        } else {
            null // null significa que el usuario no ha elegido, seguir al sistema.
        }
    }
}