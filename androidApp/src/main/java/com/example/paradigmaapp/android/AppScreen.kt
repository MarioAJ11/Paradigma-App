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

// --- Importaciones de los componentes de UI ---
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.ui.UserIcon
// --- Importación para WindowInsets ---
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * [AppScreen] es el composable principal que orquesta la visualización de la lista de podcasts
 * y el control de reproducción de audio, incluyendo tanto podcasts descargados como el streaming en vivo.
 * Ahora considera el padding de la barra de estado para evitar solapamiento.
 *
 * **Responsabilidades:**
 * - Gestiona el estado de la lista de podcasts y la carga inicial.
 * - Controla la reproducción de podcasts individuales y el streaming en vivo.
 * - Presenta una interfaz de usuario componiendo SearchBar, PodcastList y AudioPlayer,
 *   respetando los insets del sistema.
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
    var searchText by remember { mutableStateOf("") }


    suspend fun checkAndainaStreamStatus(): Boolean {
        delay(1000) // Simulación. TODO: Implementar la lógica real de la API.
        return true // Asumiendo que el stream siempre está activo para esta versión.
    }

    LaunchedEffect(Unit) {
        isAndainaStreamActive = checkAndainaStreamStatus()
        // Lógica inicial para reproducir el stream si está activo y no hay podcast seleccionado
        if (isAndainaStreamActive && !isAndainaPlaying && currentPodcast == null) {
            andainaStreamPlayer.play()
            isAndainaPlaying = andainaStreamPlayer.isPlaying()
            isPodcastPlaying = false
        }
    }

    LaunchedEffect(podcastExoPlayer.currentPosition, podcastExoPlayer.isPlaying, andainaStreamPlayer.exoPlayer?.currentPosition, andainaStreamPlayer.isPlaying()) {
        while (isActive) {
            if (currentPodcast != null) {
                if (podcastExoPlayer.duration > 0 && (podcastExoPlayer.playbackState == Player.STATE_READY || podcastExoPlayer.playbackState == Player.STATE_BUFFERING || podcastExoPlayer.isPlaying)) {
                    podcastProgress = podcastExoPlayer.currentPosition.toFloat() / podcastExoPlayer.duration.toFloat()
                    podcastDuration = podcastExoPlayer.duration
                } else if (podcastExoPlayer.playbackState == Player.STATE_ENDED) {
                    podcastProgress = 1f
                    isPodcastPlaying = false
                }
                isPodcastPlaying = podcastExoPlayer.isPlaying
                isAndainaPlaying = false // Si hay podcast, el stream no está reproduciendo
            } else {
                // Si no hay podcast, gestiona el estado del stream
                isAndainaPlaying = andainaStreamPlayer.isPlaying()
                isPodcastPlaying = false // Si hay stream, el podcast no está reproduciendo
                // TODO: Si necesitas mostrar progreso para el stream, deberías obtenerlo de andainaStreamPlayer.exoPlayer!!
            }

            delay(500)
        }
    }


    LaunchedEffect(Unit) {
        isLoadingInitial = true
        try {
            coroutineScope.launch(Dispatchers.IO) {
                val allPodcasts = archiveService.fetchAllPodcasts("mario011")
                withContext(Dispatchers.Main) {
                    initialPodcasts = allPodcasts
                    isLoadingInitial = false
                }
            }.join()

        } catch (e: Exception) {
            Timber.e("Error fetching initial podcasts: $e")
            initialPodcasts = emptyList()
            isLoadingInitial = false
        }
    }

    LaunchedEffect(currentPodcast) {
        currentPodcast?.let { podcast ->
            // Si seleccionas un podcast y el stream está activo, detenlo.
            if (isAndainaPlaying) {
                andainaStreamPlayer.stop()
                isAndainaPlaying = false
            }
            if (podcast.url.isNotEmpty()) {
                podcastExoPlayer.stop()
                podcastExoPlayer.clearMediaItems()
                podcastExoPlayer.setMediaItem(MediaItem.fromUri(podcast.url))
                podcastExoPlayer.prepare()
                podcastExoPlayer.playWhenReady = true
                isPodcastPlaying = true
                Timber.d("Preparando podcast: ${podcast.title} - URL: ${podcast.url}")
            }
        }
        // Si currentPodcast se vuelve null y el podcast player está activo, detenlo
        if (currentPodcast == null && isPodcastPlaying) {
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            isPodcastPlaying = false
            podcastProgress = 0f
            podcastDuration = 0L
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            podcastExoPlayer.release()
            andainaStreamPlayer.release()
            Timber.i("ExoPlayer and AndainaStream resources released on AppScreen dispose.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fila superior con el Buscador y el Icono de Usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // *** Asegúrate de que este modificador esté presente ***
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { searchText = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            UserIcon(
                // onClick = { /* ... */ }
            )
        }

        // Contenido principal (Lista de Podcasts o Indicador de Carga/Mensaje)
        when {
            isLoadingInitial -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            initialPodcasts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se encontraron podcasts subidos por mario011.",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                // TODO: Filtrar initialPodcasts basándose en searchText
                PodcastList(
                    podcasts = initialPodcasts, // O filteredPodcasts
                    onPodcastSelected = { podcast ->
                        currentPodcast = podcast
                    },
                    modifier = Modifier.weight(1f) // Asegúrate de que PodcastList acepta Modifier
                )
            }
        }

        // Contenedor para el botón de play flotante y el reproductor de audio
        Box(
            modifier = Modifier
                .fillMaxWidth()
            // Puedes añadir windowInsetsPadding(WindowInsets.navigationBars) si tienes
            // una barra de navegación inferior y quieres padding ahí.
            // .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // El AudioPlayer se muestra y sus controles gestionan la reproducción
            // del podcast o del stream según cuál esté activo.
            AudioPlayer(
                player = if (currentPodcast != null) podcastExoPlayer else andainaStreamPlayer.exoPlayer!!,
                isPlaying = if (currentPodcast != null) isPodcastPlaying else isAndainaPlaying,
                onPlayPauseClick = {
                    if (currentPodcast != null) {
                        // Lógica para pausar/reproducir podcast
                        if (isPodcastPlaying) {
                            podcastExoPlayer.pause()
                        } else {
                            // Si pausas el stream desde el AudioPlayer y quieres reproducir el podcast
                            // desde ahí, podrías añadir lógica para pausar el stream aquí
                            // if (isAndainaPlaying) andainaStreamPlayer.pause()
                            podcastExoPlayer.play()
                        }
                    } else {
                        // Lógica para pausar/reproducir stream
                        if (isAndainaPlaying) {
                            andainaStreamPlayer.pause()
                        } else {
                            // Si pausas el podcast desde el AudioPlayer y quieres reproducir el stream
                            // desde ahí, podrías añadir lógica para pausar el podcast aquí
                            // if (isPodcastPlaying) podcastExoPlayer.pause()
                            andainaStreamPlayer.play()
                        }
                    }
                },
                progress = if (currentPodcast != null) podcastProgress else 0f, // TODO: Considerar progreso del stream si es posible
                onProgressChange = { newProgress ->
                    if (currentPodcast != null && podcastDuration > 0) {
                        seekJob?.cancel()
                        seekJob = coroutineScope.launch {
                            delay(100)
                            podcastExoPlayer.seekTo((newProgress * podcastDuration).toLong())
                        }
                    }
                },
                isLiveStream = currentPodcast == null && isAndainaStreamActive, // Pasa si es stream en vivo (cuando no hay podcast)
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            )

            // *** Modificamos la condición aquí: Ahora solo depende de isAndainaStreamActive ***
            if (isAndainaStreamActive) {
                PlayStreaming(
                    onClick = {
                        // Lógica para iniciar/pausar el stream desde este botón flotante
                        if (isAndainaPlaying) {
                            andainaStreamPlayer.pause()
                            // isAndainaPlaying se actualizará vía LaunchedEffect
                        } else {
                            // Si inicias el stream desde este botón, detén el podcast si está activo
                            if (currentPodcast != null && isPodcastPlaying) {
                                podcastExoPlayer.stop() // O pause()
                                // Aquí podrías decidir si también quieres resetear currentPodcast a null
                                currentPodcast = null // Opcional: Deselecciona el podcast visualmente/lógicamente
                                // isPodcastPlaying y estados relacionados se actualizarán vía LaunchedEffect
                            }
                            andainaStreamPlayer.play()
                            // isAndainaPlaying se actualizará vía LaunchedEffect
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .absoluteOffset(
                            x = LocalConfiguration.current.screenWidthDp.dp - 55.dp,
                            y = -58.dp // Ajusta la posición si es necesario
                        )
                )
            }
            // *** Fin de la modificación de la condición ***
        }
    }
}