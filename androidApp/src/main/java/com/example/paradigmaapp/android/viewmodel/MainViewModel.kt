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
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import com.example.paradigmaapp.repository.contracts.ProgramaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel principal de la aplicación.
 * Gestiona el estado global de la UI, la carga inicial de datos (programas y episodios),
 * la lógica de los reproductores de audio tanto para podcasts ([podcastExoPlayer]) como para
 * el stream en vivo de Andaina FM ([andainaStreamPlayer]). También coordina y proporciona
 * acceso a otros ViewModels más específicos como [QueueViewModel], [OnGoingEpisodioViewModel]
 * y [DownloadedEpisodioViewModel].
 *
 * Es responsable de:
 * - Cargar la lista de programas y los episodios iniciales.
 * - Manejar la selección y reproducción de episodios.
 * - Controlar el estado del stream en vivo.
 * - Actualizar y persistir el progreso de reproducción.
 * - Gestionar el ciclo de vida de los reproductores de Media3.
 *
 * @property programaRepository Repositorio para obtener datos de programas.
 * @property episodioRepository Repositorio para obtener datos de episodios.
 * @property appPreferences Gestor de preferencias para persistir estados.
 * @property context Contexto de la aplicación, necesario para ExoPlayer.
 * @property queueViewModel ViewModel para la cola de reproducción.
 * @property onGoingViewModel ViewModel para los episodios en curso.
 * @property downloadedViewModel ViewModel para los episodios descargados.
 *
 * @author Mario Alguacil Juárez
 */
class MainViewModel(
    private val programaRepository: ProgramaRepository,
    private val episodioRepository: EpisodioRepository,
    private val appPreferences: AppPreferences,
    private val context: Context,
    val queueViewModel: QueueViewModel,
    val onGoingViewModel: OnGoingEpisodioViewModel,
    val downloadedViewModel: DownloadedEpisodioViewModel
) : ViewModel() {

    private val _programas = MutableStateFlow<List<Programa>>(emptyList())
    val programas: StateFlow<List<Programa>> = _programas.asStateFlow()

    private val _isLoadingProgramas = MutableStateFlow(false)
    val isLoadingProgramas: StateFlow<Boolean> = _isLoadingProgramas.asStateFlow()

    private val _programasError = MutableStateFlow<String?>(null)
    val programasError: StateFlow<String?> = _programasError.asStateFlow()

    private val _initialEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val initialEpisodios: StateFlow<List<Episodio>> = _initialEpisodios.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    private val _initialDataError = MutableStateFlow<String?>(null)
    val initialDataError: StateFlow<String?> = _initialDataError.asStateFlow()

    private val _currentPlayingEpisode = MutableStateFlow<Episodio?>(null)
    val currentPlayingEpisode: StateFlow<Episodio?> = _currentPlayingEpisode.asStateFlow()

    val podcastExoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    val andainaStreamPlayer: AndainaStream = AndainaStream(context)

    private val _isPodcastPlaying = MutableStateFlow(false)
    val isPodcastPlaying: StateFlow<Boolean> = _isPodcastPlaying.asStateFlow()

    private val _podcastProgress = MutableStateFlow(0f)
    val podcastProgress: StateFlow<Float> = _podcastProgress.asStateFlow()

    private val _podcastDuration = MutableStateFlow(0L)
    val podcastDuration: StateFlow<Long> = _podcastDuration.asStateFlow()

    private val _isAndainaStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive())
    val isAndainaStreamActive: StateFlow<Boolean> = _isAndainaStreamActive.asStateFlow()

    private val _isAndainaPlaying = MutableStateFlow(false)
    val isAndainaPlaying: StateFlow<Boolean> = _isAndainaPlaying.asStateFlow()

    private val _hasStreamLoadFailed = MutableStateFlow(false)
    val hasStreamLoadFailed: StateFlow<Boolean> = _hasStreamLoadFailed.asStateFlow()

    // StateFlow para el volumen actual del reproductor activo.
    private val _currentVolume = MutableStateFlow(1f) // Predeterminado a 100%
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    private var progressUpdateJob: Job? = null
    private val podcastPlayerListener: Player.Listener
    private val andainaPlayerListener: Player.Listener

    init {
        _currentVolume.value = podcastExoPlayer.volume // Sincronizar volumen inicial

        podcastPlayerListener = createPodcastPlayerListener()
        andainaPlayerListener = createAndainaPlayerListener()

        podcastExoPlayer.addListener(podcastPlayerListener)
        andainaStreamPlayer.addListener(andainaPlayerListener) // Asume que AndainaStream tiene addListener

        loadInitialProgramas()
        loadInitialData() // Carga episodios iniciales y restaura el estado de reproducción
        startProgressUpdates()

        observeCurrentPlayingEpisode()
        observeAndainaStreamActive()
    }

    /** Observa cambios en el episodio actual para guardar su ID y gestionar la reproducción del stream. */
    private fun observeCurrentPlayingEpisode() {
        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                appPreferences.saveCurrentEpisodeId(episode?.id)
                if (episode == null && _isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                    andainaStreamPlayer.play()
                } else if (episode != null) {
                    if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()
                }
            }
        }
    }

    /** Observa cambios en el estado de activación del stream para guardarlo y controlar el reproductor. */
    private fun observeAndainaStreamActive() {
        viewModelScope.launch {
            _isAndainaStreamActive.collect { isActive ->
                appPreferences.saveIsStreamActive(isActive) //
                if (isActive) {
                    if (_currentPlayingEpisode.value == null && !andainaStreamPlayer.isPlaying() && !_hasStreamLoadFailed.value) {
                        andainaStreamPlayer.play()
                    }
                } else {
                    andainaStreamPlayer.stop()
                }
            }
        }
    }

    /** Crea y devuelve el listener para el reproductor de podcasts (ExoPlayer). */
    private fun createPodcastPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            // Manejar errores de reproducción de podcast. Podría mostrar un mensaje al usuario.
            // Ejemplo: _initialDataError.value = "Error al reproducir: ${error.message}"
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val playedEpisode = _currentPlayingEpisode.value
                playedEpisode?.let {
                    appPreferences.saveEpisodePosition(it.id, 0L) // Marcar como completado
                    onGoingViewModel.markEpisodeAsCompleted(it.id)
                    viewModelScope.launch {
                        val nextEpisode = queueViewModel.dequeueNextEpisode(it.id)
                        if (nextEpisode != null) {
                            selectEpisode(nextEpisode, playWhenReady = true)
                        } else {
                            _currentPlayingEpisode.value = null // Limpiar si la cola está vacía
                        }
                    }
                }
            } else if (playbackState == Player.STATE_READY && !podcastExoPlayer.isPlaying) {
                _currentPlayingEpisode.value?.let {
                    appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                    onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPodcastPlaying.value = isPlaying
            if (!isPlaying && podcastExoPlayer.playbackState != Player.STATE_ENDED && podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                _currentPlayingEpisode.value?.let {
                    appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                    onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                }
            }
        }

        override fun onVolumeChanged(volume: Float) {
            if (_currentPlayingEpisode.value != null) { // Solo si un podcast es el activo
                _currentVolume.value = volume
            }
        }
    }

    /** Crea y devuelve el listener para el reproductor del stream de Andaina. */
    private fun createAndainaPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _hasStreamLoadFailed.value = true
            // Manejar errores de carga del stream. Podría mostrar un mensaje.
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isAndainaPlaying.value = isPlaying
        }

        override fun onVolumeChanged(volume: Float) {
            if (_currentPlayingEpisode.value == null) { // Solo si Andaina es el activo
                _currentVolume.value = volume
            }
        }
    }

    /** Carga la lista inicial de programas desde el repositorio. */
    fun loadInitialProgramas() {
        viewModelScope.launch {
            _isLoadingProgramas.value = true
            _programasError.value = null
            try {
                val fetchedProgramas = programaRepository.getProgramas()
                _programas.value = fetchedProgramas
            } catch (e: NoInternetException) {
                _programasError.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                _programasError.value = e.userFriendlyMessage
            } catch (e: ApiException) {
                _programasError.value = e.message ?: "Error de API al cargar programas."
            } catch (e: Exception) {
                _programasError.value = "Ocurrió un error desconocido al cargar programas."
                // Considerar registrar `e` en un sistema de monitoreo.
            } finally {
                _isLoadingProgramas.value = false
            }
        }
    }

    /** Carga los datos iniciales (episodios recientes) y restaura el estado de reproducción. */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            _initialDataError.value = null
            try {
                val episodes = episodioRepository.getAllEpisodios(page = 1, perPage = 30)
                _initialEpisodios.value = episodes

                queueViewModel.setAllAvailableEpisodes(episodes)
                downloadedViewModel.setAllAvailableEpisodes(episodes)
                onGoingViewModel.setAllAvailableEpisodes(episodes)

                val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                savedEpisodeId?.let { id ->
                    val episodeToRestore = episodes.find { it.id == id }
                        ?: episodioRepository.getEpisodio(id)
                    episodeToRestore?.let {
                        val savedPosition = appPreferences.getEpisodePosition(it.id)
                        _currentPlayingEpisode.value = it
                        prepareEpisodePlayer(it, savedPosition, playWhenReady = false)
                    }
                }
            } catch (e: NoInternetException) {
                _initialDataError.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                _initialDataError.value = e.userFriendlyMessage
            } catch (e: ApiException) {
                _initialDataError.value = e.message ?: "Error de API al cargar datos iniciales."
            } catch (e: Exception) {
                _initialDataError.value = "Ocurrió un error desconocido al cargar datos iniciales."
                // Considerar registrar `e` en un sistema de monitoreo.
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    /**
     * Selecciona un episodio para reproducción. Si ya es el episodio actual, alterna play/pause.
     *
     * @param episodio El [Episodio] a reproducir.
     * @param playWhenReady Indica si la reproducción debe comenzar inmediatamente. Por defecto es `true`.
     */
    fun selectEpisode(episodio: Episodio, playWhenReady: Boolean = true) {
        if (_currentPlayingEpisode.value?.id == episodio.id) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
            return
        }

        _currentPlayingEpisode.value = episodio
        if (andainaStreamPlayer.isPlaying()) {
            andainaStreamPlayer.stop()
        }

        val savedPosition = appPreferences.getEpisodePosition(episodio.id)
        prepareEpisodePlayer(episodio, savedPosition, playWhenReady)
    }

    /** Prepara el [podcastExoPlayer] para un episodio específico. */
    private fun prepareEpisodePlayer(episodio: Episodio, positionMs: Long, playWhenReady: Boolean) {
        val mediaPath = downloadedViewModel.getDownloadedFilePathByEpisodeId(episodio.id)
            ?: episodio.archiveUrl

        if (mediaPath == null) {
            _initialDataError.value = "No se puede reproducir '${episodio.title}'. Falta URL del archivo."
            return
        }

        try {
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(mediaPath), positionMs)
            podcastExoPlayer.prepare()
            podcastExoPlayer.playWhenReady = playWhenReady
        } catch (e: Exception) {
            _initialDataError.value = "Error al preparar la reproducción de '${episodio.title}'."
            // Considerar registrar `e` en un sistema de monitoreo.
        }
    }

    /** Gestiona el clic en el botón principal de play/pause del reproductor global. */
    fun onPlayerPlayPauseClick() {
        if (_currentPlayingEpisode.value != null) {
            if (podcastExoPlayer.isPlaying) {
                podcastExoPlayer.pause()
            } else {
                if (podcastExoPlayer.playbackState == Player.STATE_IDLE || podcastExoPlayer.playbackState == Player.STATE_ENDED) {
                    _currentPlayingEpisode.value?.let { prepareEpisodePlayer(it, appPreferences.getEpisodePosition(it.id), true) }
                } else {
                    podcastExoPlayer.play()
                }
            }
        } else {
            if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.pause() else andainaStreamPlayer.play()
            } else if (_hasStreamLoadFailed.value) {
                _hasStreamLoadFailed.value = false // Permitir reintentar
                andainaStreamPlayer.play()
            }
        }
    }

    /** Alterna el estado de activación del stream de Andaina FM. */
    fun toggleAndainaStreamActive() {
        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState // El colector se encargará de la lógica de play/stop
        if (newActiveState && _currentPlayingEpisode.value != null) {
            podcastExoPlayer.stop()
            _currentPlayingEpisode.value = null
        }
        if (newActiveState && _hasStreamLoadFailed.value) {
            _hasStreamLoadFailed.value = false // Permitir reintentar al activar
        }
    }

    /**
     * Mueve la posición de reproducción del episodio actual.
     *
     * @param progressFraction La nueva posición como una fracción (0.0 a 1.0) de la duración total.
     */
    fun seekEpisodeTo(progressFraction: Float) {
        val currentDuration = podcastExoPlayer.duration
        if (_currentPlayingEpisode.value != null && currentDuration > 0 && currentDuration != C.TIME_UNSET) {
            val seekPosition = (progressFraction * currentDuration).toLong()
            podcastExoPlayer.seekTo(seekPosition)
        }
    }

    /**
     * Establece el volumen del reproductor activo y actualiza el StateFlow de volumen.
     *
     * @param volume El nuevo nivel de volumen (0.0f a 1.0f).
     */
    fun setVolume(volume: Float) {
        val newVolume = volume.coerceIn(0f, 1f)
        // _currentVolume.value = newVolume // Se actualiza mediante el listener del player
        if (_currentPlayingEpisode.value != null) {
            podcastExoPlayer.volume = newVolume
        } else {
            andainaStreamPlayer.exoPlayer?.volume = newVolume
        }
    }

    /** Inicia un job para actualizar periódicamente el progreso de la UI del reproductor. */
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (podcastExoPlayer.playbackState == Player.STATE_READY || podcastExoPlayer.isPlaying) {
                    val duration = podcastExoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                    val currentPos = podcastExoPlayer.currentPosition
                    _podcastDuration.value = duration
                    _podcastProgress.value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f

                    if (podcastExoPlayer.isPlaying) {
                        _currentPlayingEpisode.value?.let {
                            onGoingViewModel.updateEpisodeProgress(it.id, currentPos, duration)
                        }
                    }
                }
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying() // Asegura que este estado se actualice
                delay(250) // Frecuencia de actualización (4 veces por segundo)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _currentPlayingEpisode.value?.let {
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE && podcastExoPlayer.playbackState != Player.STATE_ENDED) {
                appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
            }
        }
        podcastExoPlayer.removeListener(podcastPlayerListener)
        podcastExoPlayer.release()
        andainaStreamPlayer.removeListener(andainaPlayerListener) // Asume que AndainaStream tiene removeListener
        andainaStreamPlayer.release()
        progressUpdateJob?.cancel()
    }
}