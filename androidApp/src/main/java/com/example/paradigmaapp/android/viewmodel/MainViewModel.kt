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
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(
    private val wordpressService: WordpressService,
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

    private val _programasError = MutableStateFlow<String?>(null) // Para errores al cargar programas
    val programasError: StateFlow<String?> = _programasError.asStateFlow()

    private val _initialEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val initialEpisodios: StateFlow<List<Episodio>> = _initialEpisodios.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    private val _initialDataError = MutableStateFlow<String?>(null) // Para errores al cargar datos iniciales
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

    private var progressUpdateJob: Job? = null

    init {
        setupPodcastPlayerListeners()
        setupAndainaPlayerListeners()
        loadInitialProgramas() // Carga inicial de programas
        loadInitialData()      // Carga inicial de episodios y estado del reproductor
        startProgressUpdates()

        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                appPreferences.saveCurrentEpisodeId(episode?.id)
                if (episode == null && _isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                    andainaStreamPlayer.play()
                } else if (episode != null) {
                    if(andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()
                }
            }
        }

        viewModelScope.launch {
            _isAndainaStreamActive.collect { isActive ->
                appPreferences.saveIsStreamActive(isActive)
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

    private fun setupPodcastPlayerListeners() {
        podcastExoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "Error en ExoPlayer de Podcast: ${error.errorCodeName}")
                // Podrías añadir un StateFlow para errores del reproductor si quieres mostrarlos en la UI
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val playedEpisode = _currentPlayingEpisode.value
                    playedEpisode?.let {
                        appPreferences.saveEpisodePosition(it.id, 0L) // Guardar como completado
                        onGoingViewModel.markEpisodeAsCompleted(it.id)
                        viewModelScope.launch {
                            val nextEpisode = queueViewModel.dequeueNextEpisode(it.id)
                            if (nextEpisode != null) {
                                Timber.d("Reproduciendo siguiente episodio de la cola: ${nextEpisode.title}")
                                selectEpisode(nextEpisode, playWhenReady = true)
                            } else {
                                Timber.d("Cola vacía. Deteniendo reproducción.")
                                _currentPlayingEpisode.value = null // Limpiar episodio actual
                            }
                        }
                    }
                } else if (playbackState == Player.STATE_READY && !podcastExoPlayer.isPlaying) {
                    // Si está listo pero no reproduciendo (pausado), guardar progreso
                    _currentPlayingEpisode.value?.let {
                        appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                        onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPodcastPlaying.value = isPlaying
                if (!isPlaying) { // Si se pausa
                    _currentPlayingEpisode.value?.let {
                        appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
                        onGoingViewModel.updateEpisodeProgress(it.id, podcastExoPlayer.currentPosition, podcastExoPlayer.duration)
                    }
                }
            }
        })
    }

    private fun setupAndainaPlayerListeners() {
        andainaStreamPlayer.exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _hasStreamLoadFailed.value = true
                Timber.e(error, "Error en stream de Andaina")
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isAndainaPlaying.value = isPlaying
            }
        })
    }

    fun loadInitialProgramas() {
        viewModelScope.launch {
            _isLoadingProgramas.value = true
            _programasError.value = null // Limpiar error anterior
            try {
                val fetchedProgramas = wordpressService.getProgramas()
                _programas.value = fetchedProgramas
                Timber.d("Cargados ${fetchedProgramas.size} programas.")
            } catch (e: NoInternetException) {
                Timber.e(e, "Error de red cargando programas")
                _programasError.value = e.message ?: "Sin conexión a internet."
                _programas.value = emptyList()
            } catch (e: ServerErrorException) {
                Timber.e(e, "Error de servidor cargando programas")
                _programasError.value = e.userFriendlyMessage
                _programas.value = emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Error inesperado cargando programas")
                _programasError.value = "Ocurrió un error desconocido."
                _programas.value = emptyList()
            } finally {
                _isLoadingProgramas.value = false
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            _initialDataError.value = null // Limpiar error anterior
            try {
                // Primero, asegúrate que los programas están cargados o intenta cargarlos
                if (_programas.value.isEmpty() && _programasError.value == null && !_isLoadingProgramas.value) {
                    loadInitialProgramas() // Intenta cargar programas si no hay y no hay error previo
                    // Esperar a que la carga de programas termine si se inició aquí
                    // Esto es simplificado; en un escenario real podrías necesitar un Flow.zip o similar
                    // o que loadInitialProgramas actualice un estado que esta corutina observe.
                    // Por ahora, asumimos que si se llama, se completa o establece error.
                }

                val episodes = wordpressService.getAllEpisodios(page = 1, perPage = 30)
                _initialEpisodios.value = episodes
                Timber.d("Cargados ${episodes.size} episodios iniciales.")

                queueViewModel.setAllAvailableEpisodes(episodes)
                downloadedViewModel.setAllAvailableEpisodes(episodes)
                onGoingViewModel.setAllAvailableEpisodes(episodes)

                val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                savedEpisodeId?.let { id ->
                    val episodeToRestore = episodes.find { it.id == id }
                        ?: wordpressService.getEpisodio(id) // Fallback a buscar por ID si no está en la lista inicial
                    episodeToRestore?.let {
                        val savedPosition = appPreferences.getEpisodePosition(it.id)
                        _currentPlayingEpisode.value = it
                        prepareEpisodePlayer(it, savedPosition, playWhenReady = false) // No auto-reproducir
                        Timber.d("Episodio actual restaurado: ${it.title} en posición $savedPosition")
                    }
                }
            } catch (e: NoInternetException) {
                Timber.e(e, "Error de red cargando datos iniciales (episodios)")
                _initialDataError.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                Timber.e(e, "Error de servidor cargando datos iniciales (episodios)")
                _initialDataError.value = e.userFriendlyMessage
            } catch (e: Exception) {
                Timber.e(e, "Error inesperado cargando datos iniciales (episodios)")
                _initialDataError.value = "Ocurrió un error desconocido al cargar episodios."
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }


    fun selectEpisode(episodio: Episodio, playWhenReady: Boolean = true) {
        if (_currentPlayingEpisode.value?.id == episodio.id && podcastExoPlayer.playWhenReady == playWhenReady) {
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

    private fun prepareEpisodePlayer(episodio: Episodio, positionMs: Long, playWhenReady: Boolean) {
        val mediaPath = downloadedViewModel.getDownloadedFilePathByEpisodeId(episodio.id)
            ?: episodio.archiveUrl

        if (mediaPath == null) {
            Timber.w("No se puede reproducir el episodio ${episodio.title}, URL de archivo nula.")
            // Considera emitir un evento/error a la UI
            _programasError.value = "No se puede reproducir '${episodio.title}'. Falta URL." // Ejemplo
            return
        }

        Timber.d("Preparando reproductor para episodio ${episodio.title} con URL: $mediaPath, posición: $positionMs")
        try {
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(mediaPath), positionMs)
            podcastExoPlayer.prepare()
            podcastExoPlayer.playWhenReady = playWhenReady
        } catch (e: Exception) {
            Timber.e(e, "Error al preparar el reproductor para ${episodio.title}")
            _programasError.value = "Error al preparar '${episodio.title}'." // Ejemplo
        }
    }

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
                // Opcional: permitir reintentar el stream de Andaina
                _hasStreamLoadFailed.value = false // Resetear el error
                andainaStreamPlayer.play() // Intentar de nuevo
            }
        }
    }

    fun toggleAndainaStreamActive() {
        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState
        if (newActiveState && _currentPlayingEpisode.value != null) {
            podcastExoPlayer.stop()
            _currentPlayingEpisode.value = null
        }
        if (newActiveState && _hasStreamLoadFailed.value) { // Si se activa y había fallado, intentar de nuevo
            _hasStreamLoadFailed.value = false
            // El colector de _isAndainaStreamActive se encargará de llamar a play()
        }
    }


    fun seekEpisodeTo(progressFraction: Float) {
        val currentDuration = podcastExoPlayer.duration
        if (_currentPlayingEpisode.value != null && currentDuration > 0 && currentDuration != C.TIME_UNSET) {
            val seekPosition = (progressFraction * currentDuration).toLong()
            podcastExoPlayer.seekTo(seekPosition)
        }
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (podcastExoPlayer.playbackState == Player.STATE_READY || podcastExoPlayer.isPlaying) {
                    val duration = podcastExoPlayer.duration
                    val currentPos = podcastExoPlayer.currentPosition
                    _podcastDuration.value = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
                    _podcastProgress.value = if (_podcastDuration.value > 0) currentPos.toFloat() / _podcastDuration.value.toFloat() else 0f

                    if (podcastExoPlayer.isPlaying) {
                        _currentPlayingEpisode.value?.let {
                            onGoingViewModel.updateEpisodeProgress(it.id, currentPos, _podcastDuration.value)
                        }
                    }
                }
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying()
                delay(250)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _currentPlayingEpisode.value?.let {
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                appPreferences.saveEpisodePosition(it.id, podcastExoPlayer.currentPosition)
            }
        }
        podcastExoPlayer.release()
        andainaStreamPlayer.release()
        progressUpdateJob?.cancel()
        Timber.d("MainViewModel limpiado, reproductores liberados.")
    }
}