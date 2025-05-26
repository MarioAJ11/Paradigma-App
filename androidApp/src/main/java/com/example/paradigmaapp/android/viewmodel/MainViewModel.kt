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
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel principal para la aplicación.
 * Gestiona el estado global de la UI, la carga inicial de datos, y el reproductor de audio.
 *
 * @property wordpressService Servicio para obtener datos de WordPress.
 * @property appPreferences Gestor de preferencias.
 * @property context Contexto de la aplicación.
 * @property queueViewModel ViewModel de la cola de reproducción.
 * @property onGoingViewModel ViewModel de episodios en curso.
 * @property downloadedViewModel ViewModel de episodios descargados.
 * @author Mario Alguacil Juárez
 */
class MainViewModel(
    private val wordpressService: WordpressService,
    private val appPreferences: AppPreferences,
    private val context: Context,
    val queueViewModel: QueueViewModel, // Público para acceso desde NavGraph/UI
    val onGoingViewModel: OnGoingEpisodioViewModel, // Público
    val downloadedViewModel: DownloadedEpisodioViewModel // Público
) : ViewModel() {
    private val _programas = MutableStateFlow<List<Programa>>(emptyList())
    val programas: StateFlow<List<Programa>> = _programas.asStateFlow()

    private val _isLoadingProgramas = MutableStateFlow(false) // Inicialmente podría ser true
    val isLoadingProgramas: StateFlow<Boolean> = _isLoadingProgramas.asStateFlow()

    private val _initialEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val initialEpisodios: StateFlow<List<Episodio>> = _initialEpisodios.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

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
        loadInitialData()
        startProgressUpdates()

        // Observar cambios en el episodio actual para guardar el estado
        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                appPreferences.saveCurrentEpisodeId(episode?.id)
                if (episode == null && _isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                    andainaStreamPlayer.play()
                } else if (episode != null) {
                    // Si se selecciona un episodio, asegurarse que Andaina no esté sonando
                    if(andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()
                }
            }
        }

        // Observar cambios en la activación del stream de Andaina
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
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val playedEpisode = _currentPlayingEpisode.value
                    playedEpisode?.let {
                        appPreferences.saveEpisodePosition(it.id, 0L)
                        onGoingViewModel.markEpisodeAsCompleted(it.id)
                        viewModelScope.launch {
                            val nextEpisode = queueViewModel.dequeueNextEpisode(it.id) // Usar dequeueNextEpisode
                            if (nextEpisode != null) {
                                Timber.d("Reproduciendo siguiente episodio de la cola: ${nextEpisode.title}")
                                selectEpisode(nextEpisode, playWhenReady = true)
                            } else {
                                Timber.d("Cola vacía. Deteniendo reproducción.")
                                _currentPlayingEpisode.value = null
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
                if (!isPlaying) {
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
            try {
                val fetchedProgramas = wordpressService.getProgramas() // Debe devolver List<Programa>
                _programas.value = fetchedProgramas
            } catch (e: Exception) {
                _programas.value = emptyList() // Manejo de error
                Timber.e(e, "Error cargando programas")
            } finally {
                _isLoadingProgramas.value = false
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            try {
                val episodes = wordpressService.getAllEpisodios(page = 1, perPage = 30) // Cargar más initially
                _initialEpisodios.value = episodes
                Timber.d("Cargados ${episodes.size} episodios iniciales.")

                queueViewModel.setAllAvailableEpisodes(episodes)
                downloadedViewModel.setAllAvailableEpisodes(episodes)
                onGoingViewModel.setAllAvailableEpisodes(episodes)

                val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                savedEpisodeId?.let { id ->
                    val episodeToRestore = episodes.find { it.id == id } ?: wordpressService.getEpisodio(id)
                    episodeToRestore?.let {
                        val savedPosition = appPreferences.getEpisodePosition(it.id)
                        // No auto-reproducir al inicio, solo preparar
                        _currentPlayingEpisode.value = it
                        prepareEpisodePlayer(it, savedPosition, playWhenReady = false)
                        Timber.d("Episodio actual restaurado: ${it.title} en posición $savedPosition")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cargando episodios iniciales")
            } finally {
                _isLoadingInitial.value = false
            }
        }
    }

    fun selectEpisode(episodio: Episodio, playWhenReady: Boolean = true) {
        if (_currentPlayingEpisode.value?.id == episodio.id && podcastExoPlayer.playWhenReady == playWhenReady) {
            // Si es el mismo episodio y el estado de reproducción deseado es el actual, no hacer nada.
            // O si se hace clic en el mismo y está reproduciendo, pausar; si está pausado, reproducir.
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
            return
        }

        _currentPlayingEpisode.value = episodio // Actualiza el episodio actual
        if (andainaStreamPlayer.isPlaying()) { // Detiene Andaina si se selecciona un podcast
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
            // Aquí podrías emitir un evento a la UI para mostrar un error.
            return
        }

        Timber.d("Preparando reproductor para episodio ${episodio.title} con URL: $mediaPath, posición: $positionMs")
        podcastExoPlayer.stop() // Detener reproducción anterior
        podcastExoPlayer.clearMediaItems() // Limpiar items anteriores
        podcastExoPlayer.setMediaItem(MediaItem.fromUri(mediaPath), positionMs)
        podcastExoPlayer.prepare()
        podcastExoPlayer.playWhenReady = playWhenReady
    }

    fun onPlayerPlayPauseClick() {
        if (_currentPlayingEpisode.value != null) { // Si hay un podcast seleccionado
            if (podcastExoPlayer.isPlaying) {
                podcastExoPlayer.pause()
            } else {
                if (podcastExoPlayer.playbackState == Player.STATE_IDLE || podcastExoPlayer.playbackState == Player.STATE_ENDED) {
                    // Si no estaba listo o había terminado, prepararlo y reproducir
                    _currentPlayingEpisode.value?.let { prepareEpisodePlayer(it, appPreferences.getEpisodePosition(it.id), true) }
                } else {
                    podcastExoPlayer.play()
                }
            }
        } else { // Controlar el stream de Andaina
            if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.pause() else andainaStreamPlayer.play()
            }
        }
    }

    fun toggleAndainaStreamActive() {
        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState
        if (newActiveState && _currentPlayingEpisode.value != null) {
            podcastExoPlayer.stop() // Detener podcast si se activa el stream
            _currentPlayingEpisode.value = null
            // El colector de _isAndainaStreamActive se encargará de iniciar el play de Andaina si es necesario.
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

                    if (podcastExoPlayer.isPlaying) { // Solo actualizar "en curso" si realmente está reproduciendo
                        _currentPlayingEpisode.value?.let {
                            onGoingViewModel.updateEpisodeProgress(it.id, currentPos, _podcastDuration.value)
                        }
                    }
                }
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying()
                delay(250) // Frecuencia de actualización del progreso
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