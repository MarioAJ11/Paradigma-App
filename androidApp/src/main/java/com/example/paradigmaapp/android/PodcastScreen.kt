package com.example.paradigmaapp.android

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// Constantes de diseño
const val APP_NAME = "Paradigma App"
val primaryColor = Color(0xFFFFD700) // Amarillo
val backgroundColor = Color.Black
val textColor = Color.White

@Composable
fun PodcastScreen() {
    // 1. Definimos el podcast (usando tu URL original)
    val podcasts = listOf(
        Podcast(
            "Lost in Dreams",
            "https://archive.org/download/lost-in-dreams-abstract-chill-downtempo-cinematic-future-beats-270241/lost-in-dreams-abstract-chill-downtempo-cinematic-future-beats-270241.mp3"
        )
    )

    val context = LocalContext.current

    // 2. Estado para el podcast seleccionado
    var currentPodcast by remember { mutableStateOf(podcasts[0]) }

    // 3. Configuración robusta de ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                // Listener para depurar errores
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AUDIO", "Error: ${error.errorCodeName} - ${error.message}")
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("AUDIO", "Estado: $state")
                    }
                })
            }
    }

    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }

    // Corrutina para actualizar el progreso
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                if (exoPlayer.duration > 0) {
                    progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                    duration = exoPlayer.duration
                }
            }
            delay(1000)
        }
    }

    // 4. Efecto para cargar el audio cuando cambia el podcast
    LaunchedEffect(currentPodcast) {
        exoPlayer.run {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(currentPodcast.url))
            prepare()
            playWhenReady = true
            Log.d("AUDIO", "Reproduciendo: ${currentPodcast.url}")
        }
    }

    // 5. Liberar recursos al salir
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // 6. Interfaz de usuario
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Título
        Text(
            text = "Paradigma App",
            color = Color(0xFFFFD700),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Lista de podcasts
        PodcastList(
            podcasts = podcasts,
            onPodcastSelected = { podcast ->
                currentPodcast = podcast
            }
        )

        // Reproductor
        AudioPlayer(
            player = exoPlayer,
            onPlayPauseClick = {
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            },
            progress = progress,
            onProgressChange = { newProgress ->
                exoPlayer.seekTo((newProgress * duration).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        )
    }
}