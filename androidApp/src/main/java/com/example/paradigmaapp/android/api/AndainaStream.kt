package com.example.paradigmaapp.android.api

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.takeFrom
import io.ktor.http.parameters
import io.ktor.http.path

/**
 * Clase responsable de gestionar la reproducción del stream de audio de Andaina.
 * Utiliza ExoPlayer para la reproducción y Ktor para obtener información adicional de la radio.
 *
 * @property context El contexto de la aplicación necesario para inicializar el ExoPlayer.
 *
 * @author Mario Alguacil Juárez
 */
class AndainaStream(private val context: Context) {

    private var _exoPlayer: ExoPlayer? = null
    /**
     * Acceso público al [ExoPlayer] interno. Puede ser nulo si aún no se ha inicializado o ya se ha liberado.
     */
    val exoPlayer: ExoPlayer? get() = _exoPlayer

    private val streamUrl = "https://radio.andaina.net/8042/stream"
    private val apiUrl = "https://radio.andaina.net/"

    // CoroutineScope para gestionar las corutinas dentro de la clase
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Usa el cliente Ktor definido en NetworkModule.kt (se asume su existencia)
    private val client = ktorClient

    init {
        _exoPlayer = ExoPlayer.Builder(context).build().also {
            Timber.d("AndainaStream: ExoPlayer instance created.")
        }
    }

    /**
     * Inicia la reproducción del stream de audio.
     * Configura el [ExoPlayer] con el [MediaItem] del stream y comienza la reproducción.
     */
    fun play() {
        _exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Timber.i("AndainaStream: Started playing stream from $streamUrl.")
        } ?: Timber.w("AndainaStream: play() called but ExoPlayer is null.")
    }

    /**
     * Pausa la reproducción del stream de audio.
     * Pausa el [ExoPlayer] si está inicializado.
     */
    fun pause() {
        _exoPlayer?.pause()
        Timber.i("AndainaStream: Paused stream playback.")
    }

    /**
     * Detiene la reproducción del stream de audio y libera los [MediaItem]s.
     * Detiene el [ExoPlayer] y limpia la lista de elementos multimedia.
     */
    fun stop() {
        _exoPlayer?.stop()
        _exoPlayer?.clearMediaItems()
        Timber.i("AndainaStream: Stopped stream playback and cleared media items.")
    }

    /**
     * Libera todos los recursos asociados a la instancia de [ExoPlayer].
     * Es crucial llamar a este método cuando la instancia de [AndainaStream] ya no es necesaria
     * para evitar fugas de memoria. También cancela las corutinas lanzadas en este scope.
     */
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        Timber.i("AndainaStream: ExoPlayer resources released.")
        scope.cancel() // Cancela las corutinas cuando se libera la clase
        // Si el cliente Ktor es un singleton global, NO cerrar aquí.
        // client.close() // No descomentar si ktorClient es un singleton global
    }

    /**
     * Devuelve el estado actual de reproducción del [ExoPlayer].
     * @return `true` si el stream se está reproduciendo, `false` en caso contrario o si [ExoPlayer] es null.
     */
    fun isPlaying(): Boolean {
        return _exoPlayer?.isPlaying ?: false
    }

    /**
     * Obtiene la información actual de la radio desde la API utilizando Ktor.
     * Esta función es suspendida y debe llamarse desde una corutina.
     * Realiza una petición GET a la API y deserializa la respuesta JSON a un objeto [RadioInfo].
     *
     * @return Un objeto [RadioInfo] si la llamada a la API fue exitosa y la deserialización correcta,
     * o `null` si ocurrió un error de red, HTTP no exitoso, o deserialización.
     */
    suspend fun getRadioInfo(): RadioInfo? = withContext(Dispatchers.IO) {
        try {
            // Realiza la petición GET usando el cliente Ktor
            val response = client.get {
                // Configura la URL para la petición usando el DSL de Ktor
                url {
                    // Toma la URL base del campo apiUrl
                    takeFrom(apiUrl)
                    // Añade el path específico para el endpoint get_info.php
                    path("cp/get_info.php")
                    // Añade los parámetros de la query
                    parameters {
                        append("p", "8042")
                    }
                }
                // Ktor lanza excepciones para respuestas no exitosas por defecto con el plugin de ContentNegotiation,
                // por lo que solo necesitamos manejar la deserialización aquí.
            }
            // Deserializa el cuerpo de la respuesta a un objeto RadioInfo
            response.body<RadioInfo>()
        } catch (e: Exception) {
            // Manejar excepciones de red, respuestas HTTP con error,
            // deserialización u otras
            Timber.e(e, "AndainaStream: Error fetching radio info from API using Ktor.")
            null
        }
    }

    /**
     * Lanza una corutina para obtener la información de la radio en segundo plano.
     * Los resultados se loggean. Para actualizar la UI, se necesitaría un mecanismo
     * de comunicación entre el hilo de fondo y el hilo principal.
     */
    fun fetchRadioInfo() {
        scope.launch {
            val info = getRadioInfo()
            info?.let {
                Timber.i("AndainaStream: Radio Info - Title: ${it.title}, Listeners: ${it.onlineListeners}")
            } ?: run {
                Timber.w("AndainaStream: Could not fetch radio info or it was null.")
            }
        }
    }
}