package com.example.paradigmaapp.android

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Constantes de diseño
val primaryColor = Color(0xFFFFD700) // Amarillo
val backgroundColor = Color.Black
val textColor = Color.White // Color del texto principal

/**
 * Composable principal que representa la pantalla de Podcasts.
 * Contiene la lista de podcasts y el reproductor de audio.
 */
@Composable
fun PodcastScreen() {
    // Contexto local para acceder a recursos como el ExoPlayer.
    val context = LocalContext.current

    // Instancia del servicio para obtener podcasts de archive.org.
    val archiveService = remember { ArchiveService() }

    // Estado para almacenar la lista de podcasts obtenida del servicio.
    var podcasts by remember { mutableStateOf<List<Podcast>>(emptyList()) }

    // Estado para controlar si estamos cargando los podcasts.
    var isLoading by remember { mutableStateOf(true) }

    // Estado para el podcast seleccionado actualmente.
    var currentPodcast by remember { mutableStateOf<Podcast?>(null) }

    // Configuración de ExoPlayer.
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AUDIO", "Error de reproducción: ${error.errorCodeName} - ${error.message}")
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                    }
                })
            }
    }

    // Estados para el progreso y la duración de la reproducción.
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    // Corrutina para actualizar el progreso del Slider.
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
            delay(100)
        }
    }

    // Efecto para cargar los podcasts cuando la pantalla se compone.
    LaunchedEffect(Unit) {
        isLoading = true
        val fetchedPodcasts = archiveService.fetchAllPodcasts("mario011")
        podcasts = fetchedPodcasts
        isLoading = false

        // Auto-reproducir el primero si existe
        fetchedPodcasts.firstOrNull()?.let {
            currentPodcast = it
            exoPlayer.playWhenReady = true
        }
    }

    // Efecto para cargar el audio en ExoPlayer cuando cambia el podcast seleccionado.
    LaunchedEffect(currentPodcast) {
        currentPodcast?.let { podcast ->
            exoPlayer.run {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(podcast.url))
                prepare()
                Log.d("AUDIO", "Preparando podcast: ${podcast.title} - URL: ${podcast.url}")
            }
        }
    }

    // Efecto para liberar los recursos de ExoPlayer cuando el Composable se destruye.
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            Log.d("AUDIO", "ExoPlayer liberado.")
        }
    }

    // Interfaz de usuario principal de la pantalla.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }
            podcasts.isEmpty() -> {
                // Mensaje actualizado para mario011
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron podcasts subidos por mario011.", color = textColor, fontSize = 18.sp)
                }
            }
            else -> {
                PodcastList(
                    podcasts = podcasts,
                    onPodcastSelected = { podcast ->
                        currentPodcast = podcast
                        exoPlayer.playWhenReady = true
                    }
                )
            }
        }

        // Reproductor de audio
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
                if (duration > 0) {
                    exoPlayer.seekTo((newProgress * duration).toLong())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}