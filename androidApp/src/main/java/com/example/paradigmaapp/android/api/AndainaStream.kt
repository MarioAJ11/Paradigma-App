package com.example.paradigmaapp.android.api

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

/**
 * [AndainaStream] es una clase utilitaria robusta y bien encapsulada diseñada para
 * gestionar la reproducción del stream de audio de "Radio Andaina".
 * Utiliza la potente biblioteca [androidx.media3.exoplayer.ExoPlayer] de Android Media3, proporcionando
 * una abstracción clara para las operaciones de reproducción comunes.
 *
 * **Responsabilidades:**
 * - Inicialización y configuración del [androidx.media3.exoplayer.ExoPlayer] específico para el stream de Andaina.
 * - Control de la reproducción: iniciar (`play`), pausar (`pause`), detener (`stop`).
 * - Liberación de los recursos del [androidx.media3.exoplayer.ExoPlayer] (`release`) para evitar fugas de memoria.
 * - Consulta del estado de reproducción actual (`isPlaying`).
 *
 * **Consideraciones de Diseño:**
 * - La URL del stream (`streamUrl`) está definida como una constante privada. Para una
 * mayor flexibilidad, se podría considerar inyectarla como una dependencia o
 * obtenerla de una configuración externa.
 * - El uso de `Timber` para el logging facilita la depuración y el seguimiento del
 * comportamiento del reproductor en diferentes etapas de su ciclo de vida.
 * - La clase sigue el principio de responsabilidad única (SRP) al enfocarse exclusivamente
 * en la gestión de la reproducción de un único stream.
 *
 * **Patrones Utilizados:**
 * - **Encapsulamiento:** Oculta la complejidad de la gestión de [androidx.media3.exoplayer.ExoPlayer] y expone
 * solo las operaciones necesarias a través de métodos públicos.
 * - **Inicialización Lazy (implícita):** El [androidx.media3.exoplayer.ExoPlayer] se crea en el bloque `init` cuando
 * se instancia la clase.
 *
 * @property context El [android.content.Context] de la aplicación necesario para construir la instancia
 * de [androidx.media3.exoplayer.ExoPlayer]. Debe ser un contexto con un ciclo de vida adecuado
 * (por ejemplo, el de una `Service` o una `ComponentActivity`).
 */
class AndainaStream(private val context: Context) {

    private var _exoPlayer: ExoPlayer? = null

    /**
     * Propiedad pública de solo lectura que proporciona acceso a la instancia interna
     * de [ExoPlayer]. Esto permite a los clientes de la clase interactuar directamente
     * con el reproductor si necesitan funcionalidades más avanzadas o para observar su estado.
     *
     * **Precaución:** Se debe tener cuidado al interactuar directamente con el [ExoPlayer]
     * para no interferir con la lógica de control básica proporcionada por esta clase.
     */
    val exoPlayer: ExoPlayer? get() = _exoPlayer

    /**
     * URL estática y privada del stream de audio de "Radio Andaina".
     * Mantenerla privada fomenta la encapsulación.
     */
    private val streamUrl = "https://radio.andaina.net/8042/stream"

    /**
     * Bloque de inicialización que se ejecuta al crear una instancia de [AndainaStream].
     * Aquí se construye e inicializa la instancia de [ExoPlayer]. El uso de `also`
     * permite realizar acciones adicionales en la instancia recién creada, como el logging.
     */
    init {
        _exoPlayer = ExoPlayer.Builder(context).build().also {
            Timber.Forest.d("AndainaStream: ExoPlayer instance created.")
        }
    }

    /**
     * Inicia la reproducción del stream de audio. Si el [ExoPlayer] no está inicializado
     * o ya ha sido liberado, la operación se ignora de forma segura y se registra una
     * advertencia.
     */
    fun play() {
        _exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Timber.Forest.i("AndainaStream: Started playing stream from $streamUrl.")
        } ?: Timber.Forest.w("AndainaStream: play() called but ExoPlayer is null.")
    }

    /**
     * Pausa la reproducción del stream de audio. Si el [ExoPlayer] no está reproduciendo
     * o es nulo, la operación se ignora de forma segura.
     */
    fun pause() {
        _exoPlayer?.pause()
        Timber.Forest.i("AndainaStream: Paused stream playback.")
    }

    /**
     * Detiene la reproducción del stream de audio y libera los recursos multimedia
     * asociados al [ExoPlayer]. Si el [ExoPlayer] es nulo, la operación se ignora.
     */
    fun stop() {
        _exoPlayer?.stop()
        _exoPlayer?.clearMediaItems()
        Timber.Forest.i("AndainaStream: Stopped stream playback and cleared media items.")
    }

    /**
     * Libera todos los recursos asociados a la instancia de [ExoPlayer]. Es crucial
     * llamar a este método cuando la instancia de [AndainaStream] ya no es necesaria
     * para evitar posibles fugas de memoria. Después de la liberación, la referencia
     * a [_exoPlayer] se establece en nulo.
     */
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        Timber.Forest.i("AndainaStream: ExoPlayer resources released.")
    }

    /**
     * Devuelve el estado actual de reproducción del [ExoPlayer]. Proporciona una forma
     * sencilla para que los clientes consulten si el stream se está reproduciendo activamente.
     *
     * @return `true` si el [ExoPlayer] está actualmente reproduciendo audio, `false` en caso
     * contrario (pausado, detenido o si el [ExoPlayer] es nulo).
     */
    fun isPlaying(): Boolean {
        return _exoPlayer?.isPlaying ?: false
    }
}