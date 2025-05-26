package com.example.paradigmaapp.android.data

import android.content.Context
import android.content.SharedPreferences
// import com.example.paradigmaapp.model.Episodio // Asegúrate que el import es correcto si mueves Episodio a shared
import kotlinx.serialization.encodeToString // Import para función de extensión
import kotlinx.serialization.json.Json // Este es el import clave que falla
import timber.log.Timber

/**
 * Gestiona el almacenamiento y recuperación de preferencias de la aplicación
 * utilizando SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 * @author Mario Alguacil Juárez
 */
class AppPreferences(context: Context) {

    // Instancia de Json para serializar/deserializar.
    // Puedes configurarlo aquí si necesitas opciones específicas (pero el default suele bastar).
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
        private const val PREF_DOWNLOADED_EPISODE_IDS = "downloadedEpisodeIds_v2"
    }

    fun saveEpisodePosition(episodeId: Int, positionMillis: Long) {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap = try {
            // Usar la instancia jsonParser
            jsonParser.decodeFromString<MutableMap<String, Long>>(positionsJson)
        } catch (e: Exception) {
            Timber.e(e, "Error decodificando positionsMap")
            mutableMapOf()
        }
        positionsMap[episodeId.toString()] = positionMillis
        // Usar la instancia jsonParser
        prefs.edit().putString(PREF_EPISODE_POSITIONS, jsonParser.encodeToString(positionsMap)).apply()
        Timber.d("Posición guardada para episodio ID $episodeId en $positionMillis ms.")
    }

    fun getEpisodePosition(episodeId: Int): Long {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap = try {
            jsonParser.decodeFromString<Map<String, Long>>(positionsJson)
        } catch (e: Exception) {
            Timber.e(e, "Error decodificando positionsMap para get")
            emptyMap()
        }
        return positionsMap[episodeId.toString()] ?: 0L
    }

    fun saveCurrentEpisodeId(episodeId: Int?) {
        val editor = prefs.edit()
        if (episodeId == null) {
            editor.remove(PREF_CURRENT_EPISODE_ID)
        } else {
            editor.putInt(PREF_CURRENT_EPISODE_ID, episodeId)
        }
        editor.apply()
        Timber.d("ID del episodio actual guardado: $episodeId")
    }

    fun loadCurrentEpisodeId(): Int? {
        return if (prefs.contains(PREF_CURRENT_EPISODE_ID)) {
            prefs.getInt(PREF_CURRENT_EPISODE_ID, -1).takeIf { it != -1 }
        } else {
            null
        }
    }

    fun saveIsStreamActive(isActive: Boolean) {
        prefs.edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
        Timber.d("Estado de stream activo guardado: $isActive")
    }

    fun loadIsStreamActive(): Boolean {
        return prefs.getBoolean(PREF_IS_STREAM_ACTIVE, true)
    }

    fun saveEpisodeQueue(queueEpisodeIds: List<Int>) {
        // Usar la instancia jsonParser
        val jsonString = jsonParser.encodeToString(queueEpisodeIds)
        prefs.edit().putString(PREF_EPISODE_QUEUE_IDS, jsonString).apply()
        Timber.d("Cola de IDs de episodios guardada: $queueEpisodeIds")
    }

    fun loadEpisodeQueue(): List<Int> {
        val jsonString = prefs.getString(PREF_EPISODE_QUEUE_IDS, null)
        return if (jsonString != null) {
            try {
                jsonParser.decodeFromString<List<Int>>(jsonString)
            } catch (e: Exception) {
                Timber.e(e, "Error decodificando cola de episodios")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveDownloadedEpisodeIds(downloadedEpisodeIds: List<Int>) {
        val jsonString = jsonParser.encodeToString(downloadedEpisodeIds)
        prefs.edit().putString(PREF_DOWNLOADED_EPISODE_IDS, jsonString).apply()
        Timber.d("IDs de episodios descargados guardados: $downloadedEpisodeIds")
    }

    fun loadDownloadedEpisodeIds(): List<Int> {
        val jsonString = prefs.getString(PREF_DOWNLOADED_EPISODE_IDS, null)
        return if (jsonString != null) {
            try {
                jsonParser.decodeFromString<List<Int>>(jsonString)
            } catch (e: Exception) {
                Timber.e(e, "Error decodificando IDs de episodios descargados")
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}