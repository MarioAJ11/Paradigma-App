package com.example.paradigmaapp.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun AudioPlayer(
    player: ExoPlayer,
    onPlayPauseClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVolumeControls by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(player.volume) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Fila de controles principales
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Play/Pause - Más grande
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(
                            id = if (player.isPlaying)
                                R.mipmap.pause
                            else
                                R.mipmap.play
                        ),
                        contentDescription = if (player.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Slider de progreso
                Slider(
                    value = progress,
                    onValueChange = onProgressChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF555555),
                        activeTrackColor = Color(0xFF555555),
                        inactiveTrackColor = Color(0xFFFFFFFF)
                    )
                )

                // Tiempo del podcast
                Text(
                    text = "${formatTime(player.currentPosition)} / ${formatTime(player.duration)}",
                    color = Color(0xFF555555),
                    fontSize = 7.sp,
                    modifier = Modifier.width(60.dp)
                )

                // Botón de volumen
                IconButton(
                    onClick = { showVolumeControls = !showVolumeControls },
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(R.mipmap.volume),
                        contentDescription = "Volume Control",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Controles de volumen
            if (showVolumeControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Image(
                        painter = painterResource(R.mipmap.volume_down),
                        contentDescription = "Low volume",
                        modifier = Modifier.size(28.dp)
                    )

                    Slider(
                        value = currentVolume,
                        onValueChange = { newVolume ->
                            currentVolume = newVolume
                            player.volume = newVolume
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp), // Altura aumentada
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF555555),
                            activeTrackColor = Color(0xFF555555),
                            inactiveTrackColor = Color(0xFFFFFFFF))
                    )

                    Image(
                        painter = painterResource(R.mipmap.volume),
                        contentDescription = "High volume",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}