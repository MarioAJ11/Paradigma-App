package com.example.paradigmaapp.android

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.api.ArchiveService
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.PlayStreaming
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.podcast.PodcastList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

// Componentes de UI personalizados
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.ui.UserIcon
// Manejo de insets del sistema
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.clip
import com.example.paradigmaapp.android.ui.SettingsDialog
import androidx.media3.common.C

/**
 * Constantes para SharedPreferences
 */
private const val PREFS_NAME = "PodcastPlaybackPrefs"
private const val PREF_CURRENT_PODCAST_URL = "currentPodcastUrl"
private const val PREF_IS_STREAM_ACTIVE = "isStreamActive"
private const val PREF_PODCAST_POSITIONS = "podcastPositions" // Nuevo: Para guardar posiciones de todos los podcasts

/**
 * Obtiene las SharedPreferences para guardar el estado de reproducción y ajustes
 */
fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Guarda el progreso de reproducción de un podcast específico
 */
fun savePodcastPosition(context: Context, podcastUrl: String, positionMillis: Long) {
    val prefs = getSharedPreferences(context)
    val positions = prefs.getStringSet(PREF_PODCAST_POSITIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    // Guardamos la posición en formato "url|posición"
    positions.removeAll { it.startsWith("$podcastUrl|") }
    positions.add("$podcastUrl|$positionMillis")

    prefs.edit().apply {
        putStringSet(PREF_PODCAST_POSITIONS, positions)
        apply()
    }
    Timber.d("Saved position for podcast: $podcastUrl at $positionMillis")
}

/**
 * Obtiene la posición guardada para un podcast específico
 */
fun getPodcastPosition(context: Context, podcastUrl: String): Long {
    val prefs = getSharedPreferences(context)
    val positions = prefs.getStringSet(PREF_PODCAST_POSITIONS, mutableSetOf()) ?: return 0L

    return positions.find { it.startsWith("$podcastUrl|") }
        ?.split("|")?.get(1)?.toLongOrNull() ?: 0L
}

/**
 * Guarda el podcast actualmente seleccionado
 */
fun saveCurrentPodcast(context: Context, podcastUrl: String?) {
    getSharedPreferences(context).edit().apply {
        putString(PREF_CURRENT_PODCAST_URL, podcastUrl)
        apply()
    }
    Timber.d("Saved current podcast: $podcastUrl")
}

/**
 * Carga el podcast actualmente seleccionado
 */
fun loadCurrentPodcast(context: Context): String? {
    return getSharedPreferences(context).getString(PREF_CURRENT_PODCAST_URL, null)
}

/**
 * Guarda el estado de si el stream de Andaina debe estar activo.
 */
fun saveIsStreamActive(context: Context, isActive: Boolean) {
    getSharedPreferences(context).edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    Timber.d("Saved stream active state: $isActive")
}

/**
 * Carga el estado de si el stream de Andaina debe estar activo.
 */
fun loadIsStreamActive(context: Context): Boolean {
    return getSharedPreferences(context).getBoolean(PREF_IS_STREAM_ACTIVE, true)
}

@Composable
fun AppScreen() {
    // Contexto y servicios
    val context = LocalContext.current
    val archiveService = remember { ArchiveService() }
    val andainaStreamPlayer = remember(context) { AndainaStream(context) }

    // Estados de la UI y datos
    var initialPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var filteredPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }
    var searchText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Estados de reproducción
    var podcastProgress by remember { mutableStateOf(0f) }
    var podcastDuration by remember { mutableStateOf(0L) }
    var isPodcastPlaying by remember { mutableStateOf(false) }
    var isAndainaStreamActive by remember { mutableStateOf(loadIsStreamActive(context)) }
    var isAndainaPlaying by remember { mutableStateOf(false) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var hasStreamLoadFailed by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Player de podcast con listeners para eventos
    val podcastExoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e("Podcast ExoPlayer error: ${error.errorCodeName}")
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                currentPodcast?.let { podcast ->
                                    savePodcastPosition(context, podcast.url, 0L) // Reset al terminar
                                }
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        isPodcastPlaying = isPlaying
                        if (!isPlaying) {
                            currentPodcast?.let { podcast ->
                                savePodcastPosition(context, podcast.url, currentPosition)
                            }
                        }
                    }
                })
            }
    }

    // Listener para el player de streaming
    LaunchedEffect(andainaStreamPlayer.exoPlayer) {
        andainaStreamPlayer.exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasStreamLoadFailed = true
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isAndainaPlaying = isPlaying
            }
        })
    }

    // Cargar datos iniciales y estado guardado
    LaunchedEffect(Unit) {
        // Cargar lista de podcasts
        try {
            val allPodcasts = withContext(Dispatchers.IO) {
                archiveService.fetchAllPodcasts("mario011")
            }
            initialPodcasts = allPodcasts
            filteredPodcasts = allPodcasts
            isLoadingInitial = false

            // Cargar podcast actual guardado
            val savedPodcastUrl = loadCurrentPodcast(context)
            Timber.d("Loaded saved podcast URL: $savedPodcastUrl")
            if (savedPodcastUrl != null) {
                currentPodcast = allPodcasts.find { it.url == savedPodcastUrl }
                Timber.d("Found podcast in list: ${currentPodcast?.title}")
                currentPodcast?.let { podcast ->
                    val savedPosition = getPodcastPosition(context, podcast.url)
                    podcastExoPlayer.clearMediaItems()
                    podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcast.url), savedPosition)
                    podcastExoPlayer.prepare()
                    podcastExoPlayer.play()
                    Timber.d("Attempting to play saved podcast: ${podcast.title}")
                }
            }
        } catch (e: Exception) {
            Timber.e("Error loading podcasts: $e")
            isLoadingInitial = false
        }

        // Iniciar stream si está activo y no hay podcast cargado
        if (isAndainaStreamActive && currentPodcast == null) {
            andainaStreamPlayer.play()
        }
    }

    // Actualizar estados de reproducción periódicamente
    LaunchedEffect(podcastExoPlayer, andainaStreamPlayer) {
        while (isActive) {
            isPodcastPlaying = podcastExoPlayer.isPlaying
            isAndainaPlaying = andainaStreamPlayer.isPlaying()

            if (currentPodcast != null) {
                val duration = podcastExoPlayer.duration
                val currentPos = podcastExoPlayer.currentPosition
                podcastDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
                podcastProgress = if (podcastDuration > 0) currentPos.toFloat() / podcastDuration.toFloat() else 0f
            } else {
                podcastProgress = 0f
                podcastDuration = 0L
            }

            delay(500)
        }
    }

    // Manejar cambios en el podcast seleccionado
    LaunchedEffect(currentPodcast) {
        val podcast = currentPodcast

        if (podcast != null) {
            // Detener stream si está activo
            if (andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.stop()
            }

            // Cargar podcast con su posición guardada
            val savedPosition = getPodcastPosition(context, podcast.url)
            podcastExoPlayer.clearMediaItems()
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcast.url), savedPosition)
            podcastExoPlayer.prepare()
            podcastExoPlayer.play() // Iniciar reproducción al seleccionar

            // Guardar como podcast actual
            saveCurrentPodcast(context, podcast.url)
            Timber.d("Loaded podcast: ${podcast.title} at position: $savedPosition")
        } else {
            // Limpiar reproductor de podcast
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            saveCurrentPodcast(context, null)

            // Iniciar stream si está activo
            if (isAndainaStreamActive && !hasStreamLoadFailed) {
                andainaStreamPlayer.play()
            }
        }
    }

    // Manejar cambios en el estado activo del stream
    LaunchedEffect(isAndainaStreamActive) {
        if (isAndainaStreamActive) {
            if (currentPodcast == null && !andainaStreamPlayer.isPlaying()) {
                andainaStreamPlayer.play()
            }
        } else {
            andainaStreamPlayer.stop()
        }
        saveIsStreamActive(context, isAndainaStreamActive)
    }

    // Limpieza al salir
    DisposableEffect(Unit) {
        onDispose {
            // Guardar posición actual si hay un podcast activo
            currentPodcast?.let { podcast ->
                savePodcastPosition(context, podcast.url, podcastExoPlayer.currentPosition)
            }

            podcastExoPlayer.release()
            andainaStreamPlayer.release()
        }
    }

    // UI principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Header con barra de búsqueda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { newText -> searchText = newText },
                modifier = Modifier.weight(0.8f)
            )

            UserIcon(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .padding(1.dp)
                    .clip(CircleShape)
            )
        }

        // Lista de podcasts
        when {
            isLoadingInitial -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            filteredPodcasts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchText.isBlank()) "No hay podcasts disponibles"
                        else "No se encontraron resultados",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                PodcastList(
                    podcasts = filteredPodcasts,
                    onPodcastSelected = { podcast ->
                        currentPodcast = if (currentPodcast?.url == podcast.url) null else podcast
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Controles de reproducción
        AudioPlayer(
            player = if (currentPodcast != null) podcastExoPlayer else andainaStreamPlayer.exoPlayer,
            isPlaying = if (currentPodcast != null) isPodcastPlaying else isAndainaPlaying,
            onPlayPauseClick = {
                if (currentPodcast != null) {
                    if (isPodcastPlaying) {
                        podcastExoPlayer.pause()
                    } else {
                        podcastExoPlayer.play()
                    }
                } else {
                    if (isAndainaStreamActive) {
                        if (isAndainaPlaying) {
                            andainaStreamPlayer.pause()
                        } else {
                            andainaStreamPlayer.play()
                        }
                    }
                }
            },
            progress = podcastProgress,
            onProgressChange = { newProgress ->
                if (currentPodcast != null && podcastDuration > 0) {
                    seekJob?.cancel()
                    seekJob = coroutineScope.launch {
                        delay(100)
                        val newPosition = (newProgress * podcastDuration).toLong()
                        podcastExoPlayer.seekTo(newPosition)
                        savePodcastPosition(context, currentPodcast!!.url, newPosition)
                    }
                }
            },
            isLiveStream = currentPodcast == null,
            podcastTitle = currentPodcast?.title,
            podcastImage = R.mipmap.logo, // TODO: Reemplazar con la lógica real
            isAndainaPlaying = isAndainaPlaying,
            onPlayStreamingClick = { // Lambda para controlar el stream
                if (currentPodcast == null) {
                    if (isAndainaPlaying) {
                        andainaStreamPlayer.pause()
                    } else {
                        if (isAndainaStreamActive) {
                            andainaStreamPlayer.play()
                        }
                    }
                } else {
                    savePodcastPosition(context, currentPodcast!!.url, podcastExoPlayer.currentPosition)
                    currentPodcast = null // Esto detendrá el podcast y, si isAndainaStreamActive es true, iniciará el stream
                }
            },
            onPodcastInfoClick = {
                currentPodcast?.let {
                    if (!podcastExoPlayer.isPlaying) { // Evitar llamadas redundantes si ya está reproduciendo
                        podcastExoPlayer.play()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Diálogo de ajustes
    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false },
            isStreamActive = isAndainaStreamActive,
            onStreamActiveChanged = { isActive ->
                isAndainaStreamActive = isActive
            }
        )
    }
}