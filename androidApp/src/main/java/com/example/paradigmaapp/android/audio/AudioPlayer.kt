package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.R

/**
 * Composable que representa el reproductor de audio.
 */
@Composable
fun AudioPlayer(
    player: ExoPlayer?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    isLiveStream: Boolean = false,
    modifier: Modifier = Modifier,
    podcastTitle: String? = null,
    podcastImage: Int? = null,
    isAndainaPlaying: Boolean = false,
    onPlayStreamingClick: () -> Unit = {},
    onPodcastInfoClick: () -> Unit = {},
    onVolumeIconClick: () -> Unit = {}
) {
    var currentVolume by remember { mutableFloatStateOf(player?.volume ?: 0f) }
    var showProgressCircle by remember { mutableStateOf(false) }
    var progressCirclePosition by remember { mutableStateOf(0f) }

    LaunchedEffect(player?.volume) {
        currentVolume = player?.volume ?: 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Información del podcast/streaming
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(enabled = podcastImage != null || !podcastTitle.isNullOrEmpty()) {
                            onPodcastInfoClick()
                        }
                ) {
                    if (podcastImage != null) {
                        Image(
                            painter = painterResource(id = podcastImage),
                            contentDescription = podcastTitle ?: "Imagen del podcast",
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = podcastTitle ?: "Sin streaming en directo",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = if (!podcastTitle.isNullOrEmpty()) "Streaming en vivo: $podcastTitle" else "Sin streaming en directo",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Controles
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            painter = painterResource(id = if (isPlaying) R.mipmap.pause else R.mipmap.play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { onVolumeIconClick() }) {
                        Icon(
                            painter = painterResource(id = R.mipmap.volume),
                            contentDescription = "Volume Control",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    // Botón de PlayStreaming
                    IconButton(onClick = onPlayStreamingClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.streaming),
                            contentDescription = if (isAndainaPlaying) "Stop Streaming" else "Play Streaming",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Borde inferior para el progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                showProgressCircle = true
                                progressCirclePosition = offset.x / size.width
                                onProgressChange(progressCirclePosition)
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                progressCirclePosition = (change.position.x / size.width).coerceIn(0f, 1f)
                                onProgressChange(progressCirclePosition)
                            },
                            onDragEnd = {
                                showProgressCircle = false
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progressWidth = size.width * progress
                    val progressColor = Color(0xFFD4CFCF)
                    drawLine(
                        color = progressColor,
                        start = Offset(0f, center.y),
                        end = Offset(progressWidth, center.y),
                        strokeWidth = size.height
                    )
                    if (showProgressCircle) {
                        val circleX = size.width * progressCirclePosition
                        drawCircle(
                            color = progressColor,
                            radius = 8.dp.toPx(),
                            center = Offset(circleX, center.y)
                        )
                    }
                }
            }
        }
    }
}