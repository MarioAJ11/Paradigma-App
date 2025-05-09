package com.example.paradigmaapp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.paradigmaapp.android.audio.CircularPlayButton
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.podcast.PodcastList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * [PodcastScreen] es el composable principal que orquesta la visualización de la lista de podcasts
 * y el control de reproducción de audio, incluyendo tanto podcasts descargados como el streaming en vivo.
 *
 * **Responsabilidades:**
 * - Gestiona el estado de la lista de podcasts y la carga inicial desde [com.example.paradigmaapp.android.api.ArchiveService].
 * - Controla la reproducción de podcasts individuales utilizando una instancia de [ExoPlayer].
 * - Integra la reproducción del streaming en vivo a través de la clase [com.example.paradigmaapp.android.api.AndainaStream].
 * - Presenta una interfaz de usuario que incluye una lista de podcasts y un reproductor de audio persistente.
 * - Ofrece un botón de reproducción circular flotante para iniciar/detener la reproducción del streaming.
 *
 * **Estados Internos:**
 * - `initialPodcasts`: Lista mutable que contiene los podcasts cargados inicialmente.
 * - `isLoadingInitial`: Booleano que indica si la carga inicial de podcasts está en curso.
 * - `currentPodcast`: El podcast actualmente seleccionado para su reproducción (puede ser nulo).
 * - `podcastExoPlayer`: Instancia de [ExoPlayer] dedicada a la reproducción de podcasts.
 * - `podcastProgress`: Flotante que representa el progreso de la reproducción del podcast (entre 0.0 y 1.0).
 * - `podcastDuration`: Long que almacena la duración total del audio del podcast en milisegundos.
 * - `isPodcastPlaying`: Booleano que indica si el podcast actual se está reproduciendo.
 * - `seekJob`: Corrutina utilizada para controlar el "debounce" en la búsqueda del progreso del podcast.
 * - `isAndainaStreamActive`: Booleano que indica si el streaming de Andaina se considera activo (basado en [checkAndainaStreamStatus]).
 * - `isAndainaPlaying`: Booleano que indica si el streaming de Andaina se está reproduciendo actualmente.
 *
 * **Efectos Secundarios (`LaunchedEffect`, `DisposableEffect`):**
 * - Carga inicial de la lista de podcasts al iniciar el composable.
 * - Actualización continua del progreso y el estado de reproducción del podcast.
 * - Carga y preparación del audio del podcast seleccionado.
 * - Verificación inicial del estado del streaming y su reproducción automática si está activo.
 * - Liberación de los recursos de [ExoPlayer] y [com.example.paradigmaapp.android.api.AndainaStream] al desmontar el composable.
 *
 * **Diseño de la UI:**
 * - Utiliza un [Column] como contenedor principal para organizar los elementos verticalmente.
 * - Muestra un indicador de carga mientras se obtienen los podcasts iniciales.
 * - Presenta una lista de podcasts interactiva ([com.example.paradigmaapp.android.podcast.PodcastList]).
 * - Un [Box] se utiliza para apilar el [com.example.paradigmaapp.android.audio.CircularPlayButton] y el [com.example.paradigmaapp.android.audio.AudioPlayer], permitiendo la colocación del botón encima y a la derecha del reproductor.
 * - El [com.example.paradigmaapp.android.audio.AudioPlayer] se muestra persistentemente en la parte inferior para controlar la reproducción tanto de podcasts como del streaming.
 */
@Composable
fun AppScreen() {
    val context = LocalContext.current
    val archiveService = remember { ArchiveService() }
    val andainaStreamPlayer = remember(context) { AndainaStream(context) }
    var initialPodcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val podcastExoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e("Podcast ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                    }
                })
            }
    }
    var podcastProgress by remember { mutableStateOf(0f) }
    var podcastDuration by remember { mutableStateOf(0L) }
    var isPodcastPlaying by remember { mutableStateOf(podcastExoPlayer.isPlaying) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var isAndainaStreamActive by remember { mutableStateOf(false) }
    var isAndainaPlaying by remember { mutableStateOf(false) }

    suspend fun checkAndainaStreamStatus(): Boolean {
        delay(1000) // Simulación de lógica de verificación. TODO: Implementar la lógica real de la API.
        return true // Asumiendo que el stream siempre está activo para esta versión.
    }

    LaunchedEffect(Unit) {
        isAndainaStreamActive = checkAndainaStreamStatus()
        if (isAndainaStreamActive && !isAndainaPlaying) {
            // Solo inicia la reproducción automática si el streaming está activo y no se está reproduciendo.
            // Si ya se está reproduciendo, el usuario ya lo ha iniciado.
            andainaStreamPlayer.play()
            isAndainaPlaying = andainaStreamPlayer.isPlaying()
        }
    }

    LaunchedEffect(podcastExoPlayer.currentPosition, podcastExoPlayer.isPlaying) {
        while (isActive) {
            if (podcastExoPlayer.duration > 0 && (podcastExoPlayer.playbackState == Player.STATE_READY || podcastExoPlayer.playbackState == Player.STATE_BUFFERING || podcastExoPlayer.isPlaying)) {
                podcastProgress = podcastExoPlayer.currentPosition.toFloat() / podcastExoPlayer.duration.toFloat()
                podcastDuration = podcastExoPlayer.duration
            } else if (podcastExoPlayer.playbackState == Player.STATE_ENDED) {
                podcastProgress = 1f
                isPodcastPlaying = false
            }
            isPodcastPlaying = podcastExoPlayer.isPlaying
            delay(500)
        }
    }

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
            Timber.e("Error fetching initial podcasts: $e")
            isLoadingInitial = false
        }
    }

    LaunchedEffect(currentPodcast) {
        currentPodcast?.let { podcast ->
            andainaStreamPlayer.stop() // Detiene el streaming al seleccionar un podcast.
            if (podcast.url.isNotEmpty()) {
                podcastExoPlayer.stop()
                podcastExoPlayer.clearMediaItems()
                podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcast.url))
                podcastExoPlayer.prepare()
                podcastExoPlayer.playWhenReady = true
                Timber.d("Preparando podcast: ${podcast.title} - URL: ${podcast.url}")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            podcastExoPlayer.release()
            andainaStreamPlayer.release()
            Timber.i("ExoPlayer and AndainaStream resources released on PodcastScreen dispose.")
        }
    }

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
                    }
                )
            }
        }

        // Contenedor para el botón de play y el reproductor de audio
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Reproductor de audio principal que controla tanto podcasts como el streaming.
            AudioPlayer(
                player = if (currentPodcast != null) podcastExoPlayer else andainaStreamPlayer.exoPlayer!!,
                isPlaying = if (currentPodcast != null) isPodcastPlaying else isAndainaPlaying,
                onPlayPauseClick = {
                    if (currentPodcast != null) {
                        if (isPodcastPlaying) {
                            podcastExoPlayer.pause()
                            isPodcastPlaying = false
                        } else {
                            // Detener la reproducción del streaming si está activo
                            if (isAndainaPlaying) {
                                andainaStreamPlayer.pause()
                                isAndainaPlaying = false
                            }
                            podcastExoPlayer.play()
                            isPodcastPlaying = true
                        }
                    } else {
                        if (isAndainaPlaying) {
                            andainaStreamPlayer.pause()
                            isAndainaPlaying = false
                        } else {
                            // Detener la reproducción del podcast si está activo (aunque currentPodcast sea null, por si acaso)
                            if (isPodcastPlaying) {
                                podcastExoPlayer.pause()
                                isPodcastPlaying = false
                            }
                            andainaStreamPlayer.play()
                            isAndainaPlaying = true
                        }
                    }
                },
                progress = if (currentPodcast != null) podcastProgress else 0f,
                onProgressChange = { newProgress ->
                    if (currentPodcast != null && podcastDuration > 0) {
                        seekJob?.cancel()
                        seekJob = coroutineScope.launch {
                            delay(300)
                            podcastExoPlayer.seekTo((newProgress * podcastDuration).toLong())
                        }
                    }
                },
                isLiveStream = false, // Indica al reproductor que siempre se trata como un podcast
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
            )

            // Botón circular de Play/Pause para el streaming, posicionado absolutamente.
            if (isAndainaStreamActive) {
                CircularPlayButton(
                    onClick = {
                        if (isAndainaPlaying) {
                            andainaStreamPlayer.pause()
                            isAndainaPlaying = false
                        } else {
                            // Detener la reproducción del podcast si está activo
                            if (isPodcastPlaying) {
                                podcastExoPlayer.pause()
                                isPodcastPlaying = false
                            }
                            andainaStreamPlayer.play()
                            isAndainaPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .absoluteOffset(
                            x = LocalConfiguration.current.screenWidthDp.dp - 100.dp,
                            y = -8.dp
                        )
                )
            }
        }
    }
}