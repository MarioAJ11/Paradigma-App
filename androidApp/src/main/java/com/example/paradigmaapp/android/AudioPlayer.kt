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

/**
 * Composable que representa el reproductor de audio con controles básicos.
 * Utiliza ExoPlayer para la reproducción.
 *
 * @param player Instancia de ExoPlayer que gestiona la reproducción.
 * @param onPlayPauseClick Lambda que se ejecuta al hacer clic en el botón Play/Pause.
 * @param progress Progreso actual de la reproducción (valor entre 0.0 y 1.0).
 * @param onProgressChange Lambda que se ejecuta cuando el usuario cambia el progreso arrastrando el Slider.
 * @param modifier Modificadores para aplicar a la composición.
 */
@Composable
fun AudioPlayer(
    player: ExoPlayer,
    onPlayPauseClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado para controlar la visibilidad de los controles de volumen.
    var showVolumeControls by remember { mutableStateOf(false) }
    // Estado para almacenar el volumen actual del reproductor.
    var currentVolume by remember { mutableFloatStateOf(player.volume) }

    // Card que envuelve el reproductor, dándole un fondo y forma.
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        // Columna principal que organiza los elementos del reproductor verticalmente.
        Column(
            modifier = Modifier.padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Fila de controles principales.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Play/Pause.
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

                // Slider de progreso.
                Slider(
                    value = progress,
                    onValueChange = onProgressChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Botón de volumen.
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

            // Controles de volumen (Slider) - se muestran condicionalmente.
            if (showVolumeControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Icono de volumen bajo.
                    Image(
                        painter = painterResource(R.mipmap.volume_down),
                        contentDescription = "Low volume",
                        modifier = Modifier.size(28.dp)
                    )

                    // Slider de volumen.
                    Slider(
                        value = currentVolume,
                        onValueChange = { newVolume ->
                            currentVolume = newVolume
                            player.volume = newVolume
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Icono de volumen alto.
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