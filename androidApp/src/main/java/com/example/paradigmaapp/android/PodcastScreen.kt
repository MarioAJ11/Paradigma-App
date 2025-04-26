package com.example.paradigmaapp.android

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Composable principal [PodcastScreen] que representa la interfaz de usuario para la visualización
 * de la lista de podcasts y el control de reproducción de audio.
 *
 * Utiliza el [ArchiveService] para la obtención asíncrona de datos de podcasts desde la API
 * de archive.org y [ExoPlayer] para la gestión de audio.
 * Implementa una estrategia de carga inicial optimizada para una experiencia de usuario fluida.
 */
@Composable
fun PodcastScreen() {
    // Obtención del contexto local necesario para la inicialización de componentes dependientes del sistema.
    val context = LocalContext.current
    // Instancia del servicio [ArchiveService] para la interacción con la API de podcasts.
    val archiveService = remember { ArchiveService() }
    // Estado mutable para la lista inicial de podcasts.
    var initialPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    // Estado mutable para la visibilidad del indicador de carga inicial.
    var isLoadingInitial by remember { mutableStateOf(true) }
    // Estado mutable para el podcast actualmente seleccionado.
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }
    // Instancia de [ExoPlayer] para la reproducción de audio.
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AUDIO", "Error de reproducción: ${error.errorCodeName} - ${error.message}")
                    }
                })
            }
    }
    // Estado mutable para el progreso de la reproducción.
    var progress by remember { mutableStateOf(0f) }
    // Estado mutable para la duración total del audio.
    var duration by remember { mutableStateOf(0L) }
    // Estado mutable para indicar si el audio se está reproduciendo.
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    // CoroutineScope asociado al ciclo de vida del Composable.
    val coroutineScope = rememberCoroutineScope()
    // Job para controlar la corrutina de búsqueda (debounce).
    var seekJob by remember { mutableStateOf<Job?>(null) }

    // Efecto para actualizar el progreso y estado de reproducción.
    LaunchedEffect(exoPlayer.currentPosition, exoPlayer.isPlaying) {
        while (isActive) {
            if (exoPlayer.duration > 0 && (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING || exoPlayer.isPlaying)) {
                progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                duration = exoPlayer.duration
            } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
                progress = 1f
                isPlaying = false
            }
            isPlaying = exoPlayer.isPlaying
            delay(500)
        }
    }

    // Efecto para cargar la lista inicial de podcasts y sus detalles.
    LaunchedEffect(Unit) {
        isLoadingInitial = true
        val fetchedInitialPodcasts = mutableListOf<Podcast>()
        var page = 1
        var totalProcessed = 0
        var totalAvailable = Int.MAX_VALUE
        try {
            while (totalProcessed < totalAvailable && fetchedInitialPodcasts.size < 20) {
                val response = archiveService.fetchPage("mario011", page)
                val (podcastsFromPage, total) = archiveService.processSearchResponse(response)
                totalAvailable = total
                podcastsFromPage.forEach { podcast ->
                    fetchedInitialPodcasts.add(podcast)
                }
                totalProcessed += podcastsFromPage.size
                if (totalProcessed < totalAvailable && fetchedInitialPodcasts.size < 20) {
                    page++
                    delay(ArchiveService.DELAY_BETWEEN_REQUESTS_MS)
                }
            }
            initialPodcasts = fetchedInitialPodcasts
            isLoadingInitial = false

            coroutineScope.launch(Dispatchers.IO) {
                val allPodcasts = archiveService.fetchAllPodcasts("mario011")
                withContext(Dispatchers.Main) {
                    initialPodcasts = allPodcasts
                }
            }

        } catch (e: Exception) {
            Log.e("PodcastScreen", "Error al obtener lista inicial", e)
            isLoadingInitial = false
        }
    }

    // Efecto para cargar el audio del podcast seleccionado.
    LaunchedEffect(currentPodcast) {
        currentPodcast?.let { podcast ->
            if (podcast.url.isNotEmpty()) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(podcast.url))
                exoPlayer.prepare()
                Log.d("AUDIO", "Preparando podcast: ${podcast.title} - URL: ${podcast.url}")
            }
        }
    }

    // Efecto para liberar los recursos de ExoPlayer al destruir el Composable.
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            Log.d("AUDIO", "ExoPlayer liberado.")
        }
    }

    // Interfaz de usuario principal.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when {
            isLoadingInitial -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            initialPodcasts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se encontraron podcasts subidos por mario011.",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                PodcastList(
                    podcasts = initialPodcasts,
                    onPodcastSelected = { podcast ->
                        currentPodcast = podcast
                        exoPlayer.playWhenReady = true
                    }
                )
            }
        }

        AudioPlayer(
            player = exoPlayer,
            onPlayPauseClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            },
            progress = progress,
            onProgressChange = { newProgress ->
                seekJob?.cancel()
                seekJob = coroutineScope.launch {
                    delay(300)
                    if (duration > 0) {
                        exoPlayer.seekTo((newProgress * duration).toLong())
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}