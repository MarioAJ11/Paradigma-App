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
import com.example.paradigmaapp.model.RadioInfo
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
 * Gestiona el estado global de la UI, la carga inicial de datos, la lógica de los reproductores
 * de audio para podcasts y streaming, y coordina otros ViewModels.
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

    private val _currentVolume = MutableStateFlow(1f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    private val _preparingEpisodeId = MutableStateFlow<Int?>(null)
    val preparingEpisodeId: StateFlow<Int?> = _preparingEpisodeId.asStateFlow()

    private val _isFullScreenPlayerVisible = MutableStateFlow(false)
    val isFullScreenPlayerVisible: StateFlow<Boolean> = _isFullScreenPlayerVisible.asStateFlow()

    private val _hasNextEpisode = MutableStateFlow(false)
    val hasNextEpisode: StateFlow<Boolean> = _hasNextEpisode.asStateFlow()

    private val _hasPreviousEpisode = MutableStateFlow(false)
    val hasPreviousEpisode: StateFlow<Boolean> = _hasPreviousEpisode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(appPreferences.loadOnboardingComplete())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _andainaRadioInfo = MutableStateFlow<RadioInfo?>(null)
    val andainaRadioInfo: StateFlow<RadioInfo?> = _andainaRadioInfo.asStateFlow()

    private val _contextualPlaylist = MutableStateFlow<List<Episodio>>(emptyList())

    val podcastExoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    val andainaStreamPlayer: AndainaStream = AndainaStream(context)
    private var progressUpdateJob: Job? = null
    private var radioInfoUpdateJob: Job? = null
    private val podcastPlayerListener: Player.Listener
    private val andainaPlayerListener: Player.Listener

    init {
        _currentVolume.value = podcastExoPlayer.volume
        podcastPlayerListener = createPodcastPlayerListener()
        andainaPlayerListener = createAndainaPlayerListener()
        podcastExoPlayer.addListener(podcastPlayerListener)
        andainaStreamPlayer.addListener(andainaPlayerListener)
        loadInitialProgramas()
        loadInitialData()
        startProgressUpdates()
        startRadioInfoUpdates()
        observeCurrentPlayingEpisode()
        observeAndainaStreamActive()

        viewModelScope.launch {
            _currentPlayingEpisode.collect {
                updateNextPreviousState()
            }
        }
        viewModelScope.launch {
            queueViewModel.queueEpisodios.collect {
                updateNextPreviousState()
            }
        }
    }

    /** Carga la lista inicial de programas desde el repositorio. */
    fun loadInitialProgramas() {
        viewModelScope.launch {
            _isLoadingProgramas.value = true
            _programasError.value = null
            try {
                _programas.value = programaRepository.getProgramas()
            } catch (e: Exception) {
                _programasError.value = when (e) {
                    is NoInternetException -> e.message ?: "Sin conexión a internet."
                    is ServerErrorException -> e.userFriendlyMessage
                    is ApiException -> e.message ?: "Error de API al cargar programas."
                    else -> "Ocurrió un error desconocido."
                }
            } finally {
                _isLoadingProgramas.value = false
            }
        }
    }

    /** Carga los datos iniciales y restaura el estado de reproducción. */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            _initialDataError.value = null
            try {
                val episodes = episodioRepository.getAllEpisodios(page = 1, perPage = 30)
                _initialEpisodios.value = episodes
                queueViewModel.setAllAvailableEpisodes(episodes)

                val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                savedEpisodeId?.let { id ->
                    var episodeToRestore = appPreferences.loadEpisodioDetails(id)
                    if (episodeToRestore == null) {
                        try {
                            episodeToRestore = episodioRepository.getEpisodio(id)
                            episodeToRestore?.let { appPreferences.saveEpisodioDetails(it) }
                        } catch (e: Exception) { /* Ignorar error de red */
                        }
                    }
                    episodeToRestore?.let { episodio ->
                        val savedPosition = appPreferences.getEpisodePosition(episodio.id)
                        _currentPlayingEpisode.value = episodio
                        prepareEpisodePlayer(episodio, savedPosition, playWhenReady = false)
                    }
                }
            } catch (e: Exception) {
                _initialDataError.value = "No se pudieron cargar los últimos episodios."
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    /**
     * Marca el onboarding como completado, guarda el estado y notifica a la UI.
     */
    fun setOnboardingComplete() {
        appPreferences.saveOnboardingComplete(true)
        _onboardingCompleted.value = true
    }
    /**
     * Selecciona un episodio para reproducción. Si ya es el episodio actual, alterna play/pause.
     * @param episodio El [Episodio] a reproducir.
     * @param playWhenReady Indica si la reproducción debe comenzar inmediatamente.
     */
    fun selectEpisode(episodio: Episodio, playWhenReady: Boolean = true) {
        if (_currentPlayingEpisode.value?.id == episodio.id) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
            return
        }

        _preparingEpisodeId.value = episodio.id

        _currentPlayingEpisode.value = episodio
        if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()
        val savedPosition = appPreferences.getEpisodePosition(episodio.id)
        prepareEpisodePlayer(episodio, savedPosition, playWhenReady)
    }

    /**
     * Alterna la visibilidad del reproductor a pantalla completa.
     */
    fun toggleFullScreenPlayer() {
        _isFullScreenPlayerVisible.value = !_isFullScreenPlayerVisible.value
    }

    /**
     * Reproduce el siguiente episodio según la lógica de prioridad (cola > lista general).
     */
    fun playNextEpisode() {
        val episode = _currentPlayingEpisode.value ?: return

        val queue = queueViewModel.queueEpisodios.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }
        if (indexInQueue != -1 && indexInQueue < queue.size - 1) {
            selectEpisode(queue[indexInQueue + 1])
            return
        }

        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }
        if (indexInContextList != -1 && indexInContextList < programContextList.size - 1) {
            selectEpisode(programContextList[indexInContextList + 1])
        }
    }

    /**
     * Reproduce el episodio anterior según la lógica de prioridad (cola > lista general).
     */
    fun playPreviousEpisode() {
        val episode = _currentPlayingEpisode.value ?: return

        val queue = queueViewModel.queueEpisodios.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }
        if (indexInQueue != -1 && indexInQueue > 0) {
            selectEpisode(queue[indexInQueue - 1])
            return
        }

        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }
        if (indexInContextList != -1 && indexInContextList > 0) {
            selectEpisode(programContextList[indexInContextList - 1])
        }
    }

    /** Prepara el [podcastExoPlayer] para un episodio específico. */
    private fun prepareEpisodePlayer(episodio: Episodio, positionMs: Long, playWhenReady: Boolean) {
        val mediaPath =
            downloadedViewModel.getDownloadedFilePathByEpisodeId(episodio.id) ?: episodio.archiveUrl
        if (mediaPath == null) {
            _initialDataError.value =
                "No se puede reproducir '${episodio.title}'. Falta URL del archivo."
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
        }
    }

    /**
     * Comprueba y actualiza el estado de los botones de siguiente y anterior
     * basándose en la posición del episodio actual en la cola o en la lista general.
     */
    private fun updateNextPreviousState() {
        val episode = _currentPlayingEpisode.value ?: run {
            _hasNextEpisode.value = false
            _hasPreviousEpisode.value = false
            return
        }

        // Prioridad 1: Comprobar la cola de reproducción
        val queue = queueViewModel.queueEpisodios.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }

        if (indexInQueue != -1) {
            _hasPreviousEpisode.value = indexInQueue > 0
            _hasNextEpisode.value = indexInQueue < queue.size - 1
            return
        }

        // ---> CAMBIO: Prioridad 2: Usar la lista de contexto del programa
        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }

        if (indexInContextList != -1) {
            _hasPreviousEpisode.value = indexInContextList > 0
            _hasNextEpisode.value = indexInContextList < programContextList.size - 1
            return
        }

        _hasNextEpisode.value = false
        _hasPreviousEpisode.value = false
    }

    /** Gestiona el clic en el botón principal de play/pause del reproductor global. */
    fun onPlayerPlayPauseClick() {
        if (_currentPlayingEpisode.value != null) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
        } else {
            if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.pause() else andainaStreamPlayer.play()
            } else if (_hasStreamLoadFailed.value) {
                _hasStreamLoadFailed.value = false
                andainaStreamPlayer.play()
            }
        }
    }

    /** Alterna el estado de activación del stream de Andaina FM. */
    fun toggleAndainaStreamActive() {
        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState
        if (newActiveState && _currentPlayingEpisode.value != null) {
            podcastExoPlayer.stop()
            _currentPlayingEpisode.value = null
        }
        if (newActiveState && _hasStreamLoadFailed.value) {
            _hasStreamLoadFailed.value = false
        }
    }

    /**
     * Mueve la posición de reproducción del episodio actual.
     * @param progressFraction La nueva posición como una fracción (0.0 a 1.0) de la duración total.
     */
    fun seekEpisodeTo(progressFraction: Float) {
        val currentDuration = podcastExoPlayer.duration
        if (_currentPlayingEpisode.value != null && currentDuration > 0 && currentDuration != C.TIME_UNSET) {
            podcastExoPlayer.seekTo((progressFraction * currentDuration).toLong())
        }
    }

    /**
     * Establece el volumen del reproductor activo.
     * @param volume El nuevo nivel de volumen (0.0f a 1.0f).
     */
    fun setVolume(volume: Float) {
        val newVolume = volume.coerceIn(0f, 1f)
        if (_currentPlayingEpisode.value != null) {
            podcastExoPlayer.volume = newVolume
        } else {
            andainaStreamPlayer.exoPlayer?.volume = newVolume
        }
    }

    /**
     * Observa cambios en el episodio actual para gestionar el estado y cargar
     * la lista de contexto del programa si es necesario.
     */
    private fun observeCurrentPlayingEpisode() {
        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                // Guardar el ID del episodio actual para restaurar estado
                appPreferences.saveCurrentEpisodeId(episode?.id)

                if (episode != null) {
                    // Si el episodio es nuevo, carga su contexto de programa
                    val isNotInQueue = queueViewModel.queueEpisodios.value.none { it.id == episode.id }
                    val isNotInCurrentContext = _contextualPlaylist.value.none { it.id == episode.id }

                    if (isNotInQueue && isNotInCurrentContext) {
                        // El episodio es nuevo y no viene de la cola ni del contexto actual
                        // así que cargamos su contexto de programa.
                        episode.programaIds?.firstOrNull()?.let { programaId ->
                            try {
                                // Cargamos los episodios del mismo programa
                                val programEpisodes = episodioRepository.getEpisodiosPorPrograma(
                                    programaId = programaId,
                                    page = 1,
                                    perPage = 100 // Límite razonable para la navegación
                                )
                                _contextualPlaylist.value = programEpisodes
                            } catch (e: Exception) {
                                // Si falla la carga, el contexto será la lista de episodios iniciales
                                _contextualPlaylist.value = _initialEpisodios.value
                            }
                        } ?: run {
                            // Si el episodio no tiene programaId, usamos la lista inicial como contexto
                            _contextualPlaylist.value = _initialEpisodios.value
                        }
                    }
                    // Si el episodio que se está reproduciendo es de streaming se detiene el stream
                    if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()

                } else {
                    // Si no hay episodio, se limpia el contexto y se activa el stream si corresponde
                    _contextualPlaylist.value = emptyList()
                    if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                        andainaStreamPlayer.play()
                    }
                }
                // Siempre actualizamos el estado de los botones al cambiar de episodio
                updateNextPreviousState()
            }
        }
    }

    /** Observa cambios en el estado de activación del stream para controlar el reproductor. */
    private fun observeAndainaStreamActive() {
        viewModelScope.launch {
            _isAndainaStreamActive.collect { isActive ->
                appPreferences.saveIsStreamActive(isActive)
                if (isActive && _currentPlayingEpisode.value == null && !andainaStreamPlayer.isPlaying() && !_hasStreamLoadFailed.value) {
                    andainaStreamPlayer.play()
                } else if (!isActive) {
                    andainaStreamPlayer.stop()
                }
            }
        }
    }

    /** Crea y devuelve el listener para el reproductor de podcasts (ExoPlayer). */
    private fun createPodcastPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _initialDataError.value = "Error de reproducción: ${error.message}"
            _preparingEpisodeId.value = null
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _preparingEpisodeId.value = null
            }
            _isPodcastPlaying.value = isPlaying
            if (!isPlaying && podcastExoPlayer.playbackState != Player.STATE_ENDED && podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                _currentPlayingEpisode.value?.let { episodio ->
                    appPreferences.saveEpisodePosition(
                        episodio.id,
                        podcastExoPlayer.currentPosition
                    )
                    onGoingViewModel.addOrUpdateOnGoingEpisode(episodio)
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _currentPlayingEpisode.value?.let {
                    appPreferences.saveEpisodePosition(it.id, 0L)
                    onGoingViewModel.refrescarListaEpisodiosEnCurso()
                    viewModelScope.launch {
                        val nextEpisode = queueViewModel.dequeueNextEpisode(it.id)
                        if (nextEpisode != null) selectEpisode(
                            nextEpisode,
                            true
                        ) else _currentPlayingEpisode.value = null
                    }
                }
            }
        }

        override fun onVolumeChanged(volume: Float) {
            if (_currentPlayingEpisode.value != null) _currentVolume.value = volume
        }
    }

    /** Crea y devuelve el listener para el reproductor del stream de Andaina. */
    private fun createAndainaPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _hasStreamLoadFailed.value = true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isAndainaPlaying.value = isPlaying
        }

        override fun onVolumeChanged(volume: Float) {
            if (_currentPlayingEpisode.value == null) _currentVolume.value = volume
        }
    }

    /** Inicia un job para actualizar periódicamente el progreso de la UI del reproductor. */
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (podcastExoPlayer.isPlaying) {
                    val duration = podcastExoPlayer.duration.takeIf { it > 0 } ?: 0L
                    val currentPos = podcastExoPlayer.currentPosition
                    _podcastDuration.value = duration
                    _podcastProgress.value =
                        if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f
                    _currentPlayingEpisode.value?.let {
                        onGoingViewModel.addOrUpdateOnGoingEpisode(
                            it
                        )
                    }
                }
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying()
                delay(250)
            }
        }
    }

    /** Inicia un job que obtiene la información del stream de Andaina FM periódicamente. */
    private fun startRadioInfoUpdates() {
        radioInfoUpdateJob?.cancel()
        radioInfoUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (_isAndainaStreamActive.value || _isAndainaPlaying.value) {
                    try {
                        _andainaRadioInfo.value = andainaStreamPlayer.getRadioInfo()
                    } catch (e: Exception) {
                        _andainaRadioInfo.value = null
                    }
                }
                delay(15_000L) // Actualiza cada 15 segundos
            }
        }
    }

    /**
     * Cancela los jobs y listeners al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        _currentPlayingEpisode.value?.let { episodio ->
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                appPreferences.saveEpisodePosition(episodio.id, podcastExoPlayer.currentPosition)
                appPreferences.saveEpisodioDetails(episodio)
            }
        }
        podcastExoPlayer.removeListener(podcastPlayerListener)
        podcastExoPlayer.release()
        andainaStreamPlayer.removeListener(andainaPlayerListener)
        andainaStreamPlayer.release()
        progressUpdateJob?.cancel()
        radioInfoUpdateJob?.cancel()
    }

    /**
     * Salta hacia adelante en la reproducción del podcast actual.
     *
     * @param millis Milisegundos a adelantar (por defecto 30 segundos).
     */
    fun skipForward(millis: Long = 30000) {
        if (_currentPlayingEpisode.value != null) {
            val newPosition =
                (podcastExoPlayer.currentPosition + millis).coerceAtMost(podcastExoPlayer.duration)
            podcastExoPlayer.seekTo(newPosition)
        }
    }

    /**
     * Salta hacia atrás en la reproducción del podcast actual.
     *
     * @param millis Milisegundos a retroceder (por defecto 10 segundos).
     */
    fun rewind(millis: Long = 10000) {
        if (_currentPlayingEpisode.value != null) {
            val newPosition = (podcastExoPlayer.currentPosition - millis).coerceAtLeast(0)
            podcastExoPlayer.seekTo(newPosition)
        }
    }
}