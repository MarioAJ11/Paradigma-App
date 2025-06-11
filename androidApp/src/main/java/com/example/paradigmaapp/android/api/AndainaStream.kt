package com.example.paradigmaapp.android.api

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.model.RadioInfo
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Gestiona la reproducción del stream de audio de Andaina FM.
 * Las URLs se inyectan a través del constructor para permitir la configuración remota.
 *
 * @property context El [Context] de la aplicación.
 * @property streamUrl La URL directa del stream de audio.
 * @property apiUrl La URL base para la API de información de la radio.
 */
class AndainaStream(
    private val context: Context,
    private val streamUrl: String, // URL dinámica para el audio
    private val apiUrl: String     // URL dinámica para los metadatos
) {

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer? get() = _exoPlayer

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("AndainaStreamScope"))

    init {
        // Inicializa el reproductor de Media3 (ExoPlayer).
        _exoPlayer = ExoPlayer.Builder(context).build()
    }

    /** Inicia o reanuda la reproducción del stream. */
    fun play() {
        _exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                // Usa la URL del stream que se le pasó al ser creado.
                val mediaItem = MediaItem.fromUri(streamUrl)
                player.setMediaItem(mediaItem)
                player.prepare()
            }
            player.play()
        }
    }

    /** Obtiene la información actual de la radio (canción, oyentes, etc). */
    suspend fun getRadioInfo(): RadioInfo? = withContext(Dispatchers.IO) {
        try {
            val rawResponse: String = ktorClient.get {
                url {
                    // Usa la URL de la API que se le pasó al ser creado.
                    takeFrom(apiUrl)
                    path("cp/get_info.php")
                    parameters.append("p", "8042")
                }
            }.body()

            // Procesa la respuesta JSON especial de esta API.
            if (rawResponse.startsWith("(") && rawResponse.endsWith(")")) {
                val cleanedJson = rawResponse.substring(1, rawResponse.length - 1)
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }.decodeFromString<RadioInfo>(cleanedJson)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error al obtener info de la radio: ${e.message}")
            null
        }
    }

    fun pause() { _exoPlayer?.pause() }
    fun stop() { _exoPlayer?.stop(); _exoPlayer?.clearMediaItems() }
    fun release() { _exoPlayer?.release(); _exoPlayer = null; scope.cancel() }
    fun isPlaying(): Boolean = _exoPlayer?.isPlaying ?: false
    fun addListener(listener: Player.Listener) { _exoPlayer?.addListener(listener) }
    fun removeListener(listener: Player.Listener) { _exoPlayer?.removeListener(listener) }
}