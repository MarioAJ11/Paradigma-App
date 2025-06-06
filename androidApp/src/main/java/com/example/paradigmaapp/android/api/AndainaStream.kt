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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Gestiona la reproducción del stream de audio de Andaina FM y la obtención de metadatos relacionados.
 * Utiliza [ExoPlayer] para la reproducción del stream y [ktorClient] para las llamadas a la API
 * de información de la radio.
 *
 * Es importante llamar a [release] cuando esta instancia ya no sea necesaria para liberar
 * los recursos de ExoPlayer y cancelar las corutinas.
 *
 * @property context El [Context] de la aplicación, necesario para inicializar ExoPlayer.
 * @author Mario Alguacil Juárez
 */
class AndainaStream(private val context: Context) {

    private var _exoPlayer: ExoPlayer? = null
    /**
     * La instancia de [ExoPlayer] utilizada para la reproducción del stream.
     * Puede ser `null` si [release] ha sido llamado o si la inicialización falló.
     */
    val exoPlayer: ExoPlayer? get() = _exoPlayer

    // URLs para el stream de audio y la API de información de la radio.
    private val streamUrl = "https://radio.andaina.net/8042/stream"
    private val apiUrl = "https://radio.andaina.net/"

    // CoroutineScope para las operaciones asíncronas de esta clase.
    // Se utiliza SupervisorJob para que el fallo de una corutina hija no cancele el scope entero.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("AndainaStreamScope"))

    init {
        try {
            _exoPlayer = ExoPlayer.Builder(context).build()
        } catch (e: Exception) {
            // Considerar registrar este error en un sistema de monitoreo en producción.
            // Por ejemplo: Crashlytics.log("Failed to initialize ExoPlayer for AndainaStream")
            // Crashlytics.recordException(e)
            _exoPlayer = null // Asegurar que _exoPlayer es null si la inicialización falla.
        }
    }

    /**
     * Inicia o reanuda la reproducción del stream de audio.
     * Si ExoPlayer no está preparado, configura el [MediaItem] y lo prepara.
     */
    fun play() {
        _exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                val mediaItem = MediaItem.fromUri(streamUrl)
                player.setMediaItem(mediaItem)
                player.prepare()
            }
            player.play()
        }
    }

    /**
     * Pausa la reproducción del stream de audio.
     */
    fun pause() {
        _exoPlayer?.pause()
    }

    /**
     * Detiene la reproducción del stream y limpia los [MediaItem]s.
     * El reproductor puede ser preparado y reutilizado después de esto.
     */
    fun stop() {
        _exoPlayer?.stop()
        _exoPlayer?.clearMediaItems() // Limpia la lista de reproducción actual.
    }

    /**
     * Libera todos los recursos asociados con esta instancia de [AndainaStream],
     * incluyendo el [ExoPlayer] y cancelando todas las corutinas activas en su [scope].
     * Este método debe ser llamado cuando la instancia ya no es necesaria para prevenir fugas de memoria.
     */
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        scope.cancel() // Cancela todas las corutinas lanzadas por este scope.
    }

    /**
     * Verifica si el stream se está reproduciendo actualmente.
     *
     * @return `true` si el stream se está reproduciendo, `false` en caso contrario o si ExoPlayer es `null`.
     */
    fun isPlaying(): Boolean {
        return _exoPlayer?.isPlaying ?: false
    }

    /**
     * Obtiene la información actual de la radio (ej. canción actual, oyentes) desde la API de Andaina.
     * Esta función es suspendida y debe ser llamada desde una corutina.
     *
     * Maneja respuestas JSON no estándar (envueltas en paréntesis) limpiando la cadena antes de deserializar.
     *
     * @return Un objeto [RadioInfo] si la llamada a la API y la deserialización son exitosas;
     * `null` si ocurre algún error (red, HTTP, deserialización).
     */
    suspend fun getRadioInfo(): RadioInfo? = withContext(Dispatchers.IO) {
        try {
            val rawResponse: String = ktorClient.get {
                url {
                    takeFrom(apiUrl)
                    path("cp/get_info.php")
                    parameters.append("p", "8042")
                }
            }.body()

            if (rawResponse.startsWith("") && rawResponse.endsWith("")) {
                val cleanedJson = rawResponse.substring(1, rawResponse.length - 1)

                // Deserializar la cadena JSON limpia manualmente
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.decodeFromString<RadioInfo>(cleanedJson)
            } else {
                null // La respuesta no tiene el formato esperado
            }
        } catch (e: Exception) {
            null // Devuelve null en caso de cualquier error.
        }
    }

    /**
     * Lanza una corutina para obtener la información de la radio en segundo plano.
     * Esta función es principalmente para propósitos de ejemplo o para iniciar una
     * actualización de datos sin esperar directamente el resultado.
     * Para actualizar la UI, se requeriría un mecanismo de callback o LiveData/StateFlow.
     */
    fun fetchRadioInfoInBackground() {
        scope.launch {
            val info = getRadioInfo()
            if (info != null) {
                // Aquí podrías, por ejemplo, postear el resultado a un StateFlow si esta clase
                // fuera observada por un ViewModel, o emitir un evento.
                // Ejemplo: _radioInfoStateFlow.value = info
            } else {
                // Manejar el caso en que la info no se pudo obtener.
            }
        }
    }

    /**
     * Configura un listener para los eventos del reproductor.
     * Es responsabilidad del llamador añadir y quitar este listener.
     *
     * @param listener El [Player.Listener] a añadir.
     */
    fun addListener(listener: Player.Listener) {
        _exoPlayer?.addListener(listener)
    }

    /**
     * Elimina un listener previamente añadido de los eventos del reproductor.
     *
     * @param listener El [Player.Listener] a eliminar.
     */
    fun removeListener(listener: Player.Listener) {
        _exoPlayer?.removeListener(listener)
    }
}