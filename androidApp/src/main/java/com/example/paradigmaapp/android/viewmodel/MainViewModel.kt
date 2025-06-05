package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository // Usar la interfaz
import com.example.paradigmaapp.repository.contracts.ProgramaRepository // Usar la interfaz
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel principal de mi aplicación.
 * Gestiono el estado global de la UI, la carga inicial de datos (programas y episodios),
 * la lógica del reproductor de audio tanto para podcasts (ExoPlayer) como para el stream
 * en vivo de Andaina FM. También coordino y proporciono acceso a otros ViewModels más
 * específicos como el de la cola, episodios en curso y descargados.
 *
 * @author Mario Alguacil Juárez
 */
class MainViewModel(
    // Ahora dependo de las abstracciones del repositorio en lugar de la implementación concreta.
    private val programaRepository: ProgramaRepository,
    private val episodioRepository: EpisodioRepository,
    private val appPreferences: AppPreferences,
    private val context: Context,
    // Estos ViewModels gestionan partes específicas del estado y la lógica.
    val queueViewModel: QueueViewModel,
    val onGoingViewModel: OnGoingEpisodioViewModel,
    val downloadedViewModel: DownloadedEpisodioViewModel
) : ViewModel() {

    // Estado para la lista de programas.
    private val _programas = MutableStateFlow<List<Programa>>(emptyList())
    val programas: StateFlow<List<Programa>> = _programas.asStateFlow()

    // Indica si se están cargando los programas.
    private val _isLoadingProgramas = MutableStateFlow(false)
    val isLoadingProgramas: StateFlow<Boolean> = _isLoadingProgramas.asStateFlow()

    // Almacena mensajes de error relacionados con la carga de programas.
    private val _programasError = MutableStateFlow<String?>(null)
    val programasError: StateFlow<String?> = _programasError.asStateFlow()

    // Estado para la lista inicial de episodios (ej. los más recientes).
    private val _initialEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val initialEpisodios: StateFlow<List<Episodio>> = _initialEpisodios.asStateFlow()

    // Indica si los datos iniciales (incluyendo episodios) se están cargando.
    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    // Almacena mensajes de error relacionados con la carga de datos iniciales.
    private val _initialDataError = MutableStateFlow<String?>(null)
    val initialDataError: StateFlow<String?> = _initialDataError.asStateFlow()

    // El episodio que se está reproduciendo o está pausado actualmente.
    private val _currentPlayingEpisode = MutableStateFlow<Episodio?>(null)
    val currentPlayingEpisode: StateFlow<Episodio?> = _currentPlayingEpisode.asStateFlow()

    // Instancia de ExoPlayer para la reproducción de podcasts.
    val podcastExoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    // Instancia para gestionar el stream de Andaina FM.
    val andainaStreamPlayer: AndainaStream = AndainaStream(context)

    // Indica si un podcast se está reproduciendo.
    private val _isPodcastPlaying = MutableStateFlow(false)
    val isPodcastPlaying: StateFlow<Boolean> = _isPodcastPlaying.asStateFlow()

    // Progreso actual del podcast (0.0 a 1.0).
    private val _podcastProgress = MutableStateFlow(0f)
    val podcastProgress: StateFlow<Float> = _podcastProgress.asStateFlow()

    // Duración total del podcast actual en milisegundos.
    private val _podcastDuration = MutableStateFlow(0L)
    val podcastDuration: StateFlow<Long> = _podcastDuration.asStateFlow()

    // Indica si el usuario ha activado el modo de streaming de Andaina.
    private val _isAndainaStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive())
    val isAndainaStreamActive: StateFlow<Boolean> = _isAndainaStreamActive.asStateFlow()

    // Indica si el stream de Andaina se está reproduciendo.
    private val _isAndainaPlaying = MutableStateFlow(false)
    val isAndainaPlaying: StateFlow<Boolean> = _isAndainaPlaying.asStateFlow()

    // Indica si ha habido un error al cargar el stream de Andaina.
    private val _hasStreamLoadFailed = MutableStateFlow(false)
    val hasStreamLoadFailed: StateFlow<Boolean> = _hasStreamLoadFailed.asStateFlow()

    // Job para la actualización periódica del progreso del reproductor.
    private var progressUpdateJob: Job? = null

    // Volumen del controlador, lo guarda.
    private val _currentVolume = MutableStateFlow(1f) // Volumen inicial (0.0f a 1.0f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()


    init {
        setupPodcastPlayerListeners()
        setupAndainaPlayerListeners()
        loadInitialProgramas()
        loadInitialData()
        startProgressUpdates()

        // Inicializar el volumen del player y el StateFlow
        val initialPlayerVolume = podcastExoPlayer.volume
        _currentVolume.value = initialPlayerVolume

        // Observo cambios en el episodio actual para guardar su ID y gestionar la reproducción del stream.
        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                appPreferences.saveCurrentEpisodeId(episode?.id)
                if (episode == null && _isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                    andainaStreamPlayer.play() // Si no hay episodio y el stream está activo, reproducir stream.
                } else if (episode != null) {
                    if(andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop() // Si hay episodio, detener stream.
                }
            }
        }

        // Observo cambios en el estado de activación del stream para guardarlo y controlar el reproductor.
        viewModelScope.launch {
            _isAndainaStreamActive.collect { isActive ->
                appPreferences.saveIsStreamActive(isActive)
                if (isActive) {
                    if (_currentPlayingEpisode.value == null && !andainaStreamPlayer.isPlaying() && !_hasStreamLoadFailed.value) {
                        andainaStreamPlayer.play() // Activar y reproducir stream si no hay podcast.
                    }
                } else {
                    andainaStreamPlayer.stop() // Desactivar y detener stream.
                }
            }
        }
    }

    /**
     * Establece el volumen del reproductor activo y actualiza el StateFlow.
     */
    fun setVolume(volume: Float) {
        val newVolume = volume.coerceIn(0f, 1f)
        _currentVolume.value = newVolume
        if (_currentPlayingEpisode.value != null) {
            podcastExoPlayer.volume = newVolume
        } else {
            andainaStreamPlayer.exoPlayer?.volume = newVolume
        }
    }

    // Configuro los listeners para el reproductor de podcasts (ExoPlayer).
    private fun setupPodcastPlayerListeners() {
        podcastExoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Aquí podría gestionar errores específicos del reproductor de podcasts.
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Cuando un episodio termina, lo marco como completado y reproduzco el siguiente de la cola.
                    val playedEpisode = _currentPlayingEpisode.value
                    playedEpisode?.let {
                        appPreferences.saveEpisodePosition(it.id, 0L) // Guardo la posición como 0 (completado).
                        onGoingViewModel.markEpisodeAsCompleted(it.id)
                        viewModelScope.launch {
                            val nextEpisode = queueViewModel.dequeueNextEpisode(it.id)
                            if (nextEpisode != null) {
                                selectEpisode(nextEpisode, playWhenReady = true)
                            } else {
                                _currentPlayingEpisode.value = null // Limpio el episodio actual si la cola está vacía.
                            }
                        }
                    }
                } else if (playbackState == Player.STATE_READY && !podcastExoPlayer.isPlaying) {
                    // Si está listo pero pausado, guardo la posición.
                    _currentPlayingEpisode.value?.let {
                        appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                        onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPodcastPlaying.value = isPlaying
                if (!isPlaying) { // Si se pausa, guardo la posición.
                    _currentPlayingEpisode.value?.let {
                        appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                        onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                    }
                }
            }

            override fun onVolumeChanged(volume: Float) {
                _currentVolume.value = volume
            }
        })
    }

    // Configuro los listeners para el reproductor del stream de Andaina.
    private fun setupAndainaPlayerListeners() {
        andainaStreamPlayer.exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _hasStreamLoadFailed.value = true // Marco que el stream falló al cargar.
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isAndainaPlaying.value = isPlaying // Actualizo el estado de reproducción del stream.
            }

            override fun onVolumeChanged(volume: Float) {
                if (_currentPlayingEpisode.value == null) { // Solo actualizar si Andaina es el activo
                    _currentVolume.value = volume
                }
            }
        })
    }

    /**
     * Carga la lista inicial de programas desde el repositorio.
     * Actualiza los estados de carga y error correspondientes.
     */
    fun loadInitialProgramas() {
        viewModelScope.launch {
            _isLoadingProgramas.value = true
            _programasError.value = null // Limpio errores previos.
            try {
                // Uso la interfaz del repositorio, no la implementación concreta.
                val fetchedProgramas = programaRepository.getProgramas()
                _programas.value = fetchedProgramas
            } catch (e: NoInternetException) {
                _programasError.value = e.message ?: "Sin conexión a internet."
                _programas.value = emptyList()
            } catch (e: ServerErrorException) {
                _programasError.value = e.userFriendlyMessage
                _programas.value = emptyList()
            } catch (e: Exception) {
                _programasError.value = "Ocurrió un error desconocido al cargar programas."
                _programas.value = emptyList()
            } finally {
                _isLoadingProgramas.value = false
            }
        }
    }

    /**
     * Carga los datos iniciales de la aplicación, incluyendo los episodios más recientes
     * y restaura el estado del episodio que se estaba reproduciendo.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            _initialDataError.value = null // Limpio errores previos.
            try {
                // Cargo los episodios iniciales usando la interfaz del repositorio.
                val episodes = episodioRepository.getAllEpisodios(page = 1, perPage = 30)
                _initialEpisodios.value = episodes

                // Actualizo los ViewModels de cola, en curso y descargados con la lista completa de episodios.
                queueViewModel.setAllAvailableEpisodes(episodes)
                downloadedViewModel.setAllAvailableEpisodes(episodes)
                onGoingViewModel.setAllAvailableEpisodes(episodes)

                // Intento restaurar el episodio que se estaba reproduciendo la última vez.
                val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                savedEpisodeId?.let { id ->
                    // Busco el episodio en la lista cargada o lo obtengo del repositorio si no está.
                    val episodeToRestore = episodes.find { it.id == id }
                        ?: episodioRepository.getEpisodio(id)
                    episodeToRestore?.let {
                        val savedPosition = appPreferences.getEpisodePosition(it.id)
                        _currentPlayingEpisode.value = it
                        // Preparo el reproductor pero no inicio la reproducción automáticamente.
                        prepareEpisodePlayer(it, savedPosition, playWhenReady = false)
                    }
                }
            } catch (e: NoInternetException) {
                _initialDataError.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                _initialDataError.value = e.userFriendlyMessage
            } catch (e: Exception) {
                _initialDataError.value = "Ocurrió un error desconocido al cargar episodios."
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    /**
     * Selecciona un episodio para reproducción o reanuda/pausa el actual.
     * @param episodio El episodio a reproducir.
     * @param playWhenReady Indica si la reproducción debe comenzar inmediatamente.
     */
    fun selectEpisode(episodio: Episodio, playWhenReady: Boolean = true) {
        // Si es el mismo episodio y se pide el mismo estado de reproducción, alterno play/pause.
        if (_currentPlayingEpisode.value?.id == episodio.id && podcastExoPlayer.playWhenReady == playWhenReady) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
            return
        }

        _currentPlayingEpisode.value = episodio // Actualizo el episodio actual.
        if (andainaStreamPlayer.isPlaying()) { // Detengo el stream de Andaina si estaba activo.
            andainaStreamPlayer.stop()
        }

        val savedPosition = appPreferences.getEpisodePosition(episodio.id) // Obtengo la posición guardada.
        prepareEpisodePlayer(episodio, savedPosition, playWhenReady)
    }

    // Prepara el ExoPlayer para un episodio específico.
    private fun prepareEpisodePlayer(episodio: Episodio, positionMs: Long, playWhenReady: Boolean) {
        // Obtengo la ruta del archivo: descargado localmente o URL de streaming.
        val mediaPath = downloadedViewModel.getDownloadedFilePathByEpisodeId(episodio.id)
            ?: episodio.archiveUrl

        if (mediaPath == null) {
            // Si no hay ruta, no se puede reproducir. Podría mostrar un error al usuario.
            _programasError.value = "No se puede reproducir '${episodio.title}'. Falta la URL del archivo."
            return
        }

        try {
            podcastExoPlayer.stop() // Detengo la reproducción anterior.
            podcastExoPlayer.clearMediaItems() // Limpio la lista de reproducción.
            // Configuro el nuevo MediaItem con la posición de inicio guardada.
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(mediaPath), positionMs)
            podcastExoPlayer.prepare() // Preparo el reproductor.
            podcastExoPlayer.playWhenReady = playWhenReady // Indico si debe empezar a reproducir.
        } catch (e: Exception) {
            _programasError.value = "Error al preparar la reproducción de '${episodio.title}'."
        }
    }

    /**
     * Gestiona el clic en el botón principal de play/pause del reproductor.
     * Alterna entre reproducir/pausar el podcast o el stream de Andaina.
     */
    fun onPlayerPlayPauseClick() {
        if (_currentPlayingEpisode.value != null) { // Si hay un podcast seleccionado.
            if (podcastExoPlayer.isPlaying) {
                podcastExoPlayer.pause()
            } else {
                // Si no estaba listo o había terminado, lo preparo y reproduzco.
                if (podcastExoPlayer.playbackState == Player.STATE_IDLE || podcastExoPlayer.playbackState == Player.STATE_ENDED) {
                    _currentPlayingEpisode.value?.let { prepareEpisodePlayer(it, appPreferences.getEpisodePosition(it.id), true) }
                } else {
                    podcastExoPlayer.play()
                }
            }
        } else { // Si no hay podcast, controlo el stream de Andaina.
            if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.pause() else andainaStreamPlayer.play()
            } else if (_hasStreamLoadFailed.value) {
                // Permito reintentar el stream si había fallado.
                _hasStreamLoadFailed.value = false
                andainaStreamPlayer.play()
            }
        }
    }

    /**
     * Alterna el estado de activación del stream de Andaina.
     * Si se activa y hay un podcast reproduciéndose, el podcast se detiene.
     */
    fun toggleAndainaStreamActive() {
        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState
        if (newActiveState && _currentPlayingEpisode.value != null) {
            podcastExoPlayer.stop() // Detengo el podcast si se activa el stream.
            _currentPlayingEpisode.value = null
        }
        if (newActiveState && _hasStreamLoadFailed.value) { // Si se activa y el stream había fallado, intento de nuevo.
            _hasStreamLoadFailed.value = false
            // La reproducción se iniciará por el colector de _isAndainaStreamActive.
        }
    }

    /**
     * Mueve la posición de reproducción del episodio actual.
     * @param progressFraction La nueva posición como una fracción (0.0 a 1.0) de la duración total.
     */
    fun seekEpisodeTo(progressFraction: Float) {
        val currentDuration = podcastExoPlayer.duration
        // Solo busco si hay un episodio, la duración es válida y mayor que cero.
        if (_currentPlayingEpisode.value != null && currentDuration > 0 && currentDuration != C.TIME_UNSET) {
            val seekPosition = (progressFraction * currentDuration).toLong()
            podcastExoPlayer.seekTo(seekPosition)
        }
    }

    // Inicia un job para actualizar periódicamente el progreso de la UI del reproductor.
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel() // Cancelo jobs anteriores.
        progressUpdateJob = viewModelScope.launch {
            while (isActive) { // Mientras el ViewModel esté activo.
                // Si el reproductor de podcast está listo o reproduciendo, actualizo su progreso.
                if (podcastExoPlayer.playbackState == Player.STATE_READY || podcastExoPlayer.isPlaying) {
                    val duration = podcastExoPlayer.duration
                    val currentPos = podcastExoPlayer.currentPosition
                    _podcastDuration.value = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
                    _podcastProgress.value = if (_podcastDuration.value > 0) currentPos.toFloat() / _podcastDuration.value.toFloat() else 0f

                    // Solo actualizo el progreso en OnGoingEpisodioViewModel si realmente está reproduciendo.
                    if (podcastExoPlayer.isPlaying) {
                        _currentPlayingEpisode.value?.let {
                            onGoingViewModel.updateEpisodeProgress(it.id, currentPos, _podcastDuration.value)
                        }
                    }
                }
                // Actualizo el estado de reproducción del stream de Andaina.
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying()
                delay(250) // Frecuencia de actualización (ej. 4 veces por segundo).
            }
        }
    }

    // Se llama cuando el ViewModel está a punto de ser destruido.
    // Libero recursos, especialmente los reproductores.
    override fun onCleared() {
        super.onCleared()
        // Guardo la posición del episodio actual si se estaba reproduciendo algo.
        _currentPlayingEpisode.value?.let {
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
            }
        }
        podcastExoPlayer.release() // Libero ExoPlayer.
        andainaStreamPlayer.release() // Libero el reproductor de Andaina.
        progressUpdateJob?.cancel() // Cancelo el job de actualización de progreso.
    }
}