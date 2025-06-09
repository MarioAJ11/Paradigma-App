package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerScreen(
    mainViewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val currentEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val progress by mainViewModel.podcastProgress.collectAsState()
    val duration by mainViewModel.podcastDuration.collectAsState()
    val hasNextEpisode by mainViewModel.hasNextEpisode.collectAsState()
    val hasPreviousEpisode by mainViewModel.hasPreviousEpisode.collectAsState()

    // Formateador de tiempo
    fun formatTime(millis: Long): String {
        return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo ahora") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            currentEpisode?.let { episode ->
                // Imagen del episodio
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = "Portada de ${episode.title.unescapeHtmlEntities()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.logo_foreground),
                    placeholder = painterResource(R.mipmap.logo_foreground)
                )

                // Título y programa
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = episode.title.unescapeHtmlEntities(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paradigma Media", // Puedes obtener el nombre del programa si lo tienes
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Barra de progreso y tiempos
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progress,
                        onValueChange = { mainViewModel.seekEpisodeTo(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime((progress * duration).toLong()),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(formatTime(duration), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Controles de reproducción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón de Episodio Anterior
                    IconButton(
                        onClick = { mainViewModel.playPreviousEpisode() },
                        modifier = Modifier.size(56.dp),
                        enabled = hasPreviousEpisode
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Episodio Anterior",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Botón de Play/Pausa (sin cambios)
                    IconButton(
                        onClick = { mainViewModel.onPlayerPlayPauseClick() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botón de Siguiente Episodio
                    IconButton(
                        onClick = { mainViewModel.playNextEpisode() },
                        modifier = Modifier.size(56.dp),
                        enabled = hasNextEpisode
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente Episodio",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}