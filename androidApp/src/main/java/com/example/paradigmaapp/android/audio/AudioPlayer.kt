package com.example.paradigmaapp.android.audio;

import androidx.compose.foundation.Canvas;
import androidx.compose.foundation.background;
import androidx.compose.foundation.clickable;
import androidx.compose.foundation.gestures.detectHorizontalDragGestures;
import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.shape.RoundedCornerShape;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset;
import androidx.compose.ui.input.pointer.pointerInput;
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource;
import androidx.compose.ui.unit.dp;
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R;

/**
 * Composable que representa la interfaz de un reproductor de audio compacto.
 * Este componente muestra información básica sobre la reproducción actual (título del podcast/streaming,
 * imagen opcional) y proporciona controles esenciales como reproducir/pausar, control de volumen
 * y un botón para iniciar/detener la reproducción del streaming en vivo.
 * También incluye una barra de progreso interactiva que permite al usuario navegar a diferentes
 * puntos de la reproducción (solo para contenido no en vivo).
 *
 * @param player El {@link ExoPlayer} asociado para controlar la reproducción. Puede ser nulo.
 * @param isPlaying Estado booleano que indica si el audio se está reproduciendo actualmente.
 * @param onPlayPauseClick Lambda que se invoca al hacer clic en el botón de reproducir/pausar.
 * @param progress Un valor de punto flotante entre 0.0 y 1.0 que representa el progreso de la reproducción.
 * @param onProgressChange Lambda que se invoca cuando el usuario interactúa con la barra de progreso,
 * proporcionando el nuevo valor de progreso (entre 0.0 y 1.0).
 * @param isLiveStream Booleano que indica si el contenido que se está reproduciendo es un streaming en vivo.
 * Esto deshabilita la interacción con la barra de progreso. Por defecto es `false`.
 * @param modifier Modificador opcional para personalizar el diseño de este composable.
 * @param podcastTitle Título opcional del podcast o del streaming que se está reproduciendo.
 * @param podcastImage Recurso de imagen opcional para mostrar junto al título del podcast.
 * @param isAndainaPlaying Estado booleano que indica si el streaming de "Andaina" se está reproduciendo.
 * @param onPlayStreamingClick Lambda que se invoca al hacer clic en el botón de iniciar/detener el streaming.
 * @param onPodcastInfoClick Lambda que se invoca al hacer clic en la información del podcast (título/imagen).
 * @param onVolumeIconClick Lambda que se invoca al hacer clic en el icono de volumen para mostrar el control de volumen detallado.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun AudioPlayer(
    player: Player?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    isLiveStream: Boolean,
    podcastTitle: String?,
    podcastImage: String?,
    isAndainaPlaying: Boolean,
    onPlayStreamingClick: () -> Unit,
    onPodcastInfoClick: () -> Unit,
    onVolumeIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    /**
     * Estado local para mantener el volumen actual del reproductor.
     */
    var currentVolume by remember { mutableFloatStateOf(player?.volume ?: 0f) }
    /**
     * Estado local para controlar la visibilidad del círculo de progreso durante el arrastre.
     */
    var showProgressCircle by remember { mutableStateOf(false) }
    /**
     * Estado local para almacenar la posición del círculo de progreso durante el arrastre.
     */
    var progressCirclePosition by remember { mutableFloatStateOf(0f) }

    /**
     * Efecto lanzado cuando cambia el volumen del reproductor externo para actualizar el estado local.
     */
    LaunchedEffect(player?.volume) {
        currentVolume = player?.volume ?: 0f
    }

    // <-- Nuevo: Obtén los colores del tema fuera del bloque Canvas
    val progressColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary


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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = podcastImage != null || !podcastTitle.isNullOrEmpty()) {
                            onPodcastInfoClick()
                        }
                ) {
                    if (podcastImage != null) {
                        // Si podcastImage es una URL de String
                        AsyncImage(
                            model = podcastImage, // Aquí se pasaría la URL
                            contentDescription = "Podcast Cover",
                            modifier = Modifier
                                .size(64.dp) // O el tamaño que necesites
                                .clip(RoundedCornerShape(4.dp)), // Para esquinas redondeadas
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.imagen),
                            placeholder = painterResource(R.mipmap.imagen) // Imagen de placeholder mientras carga
                        )
                        Text(
                            text = podcastTitle ?: "Sin streaming en directo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onPrimaryColor
                        )
                    } else {
                        Text(
                            text = if (!podcastTitle.isNullOrEmpty()) "Streaming en vivo: $podcastTitle" else "Sin streaming en directo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onPrimaryColor
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
                            modifier = Modifier.size(32.dp),
                            tint = onPrimaryColor
                        )
                    }
                    IconButton(onClick = { onVolumeIconClick() }) {
                        Icon(
                            painter = painterResource(id = R.mipmap.volume),
                            contentDescription = "Control de Volumen",
                            modifier = Modifier.size(32.dp),
                            tint = onPrimaryColor
                        )
                    }
                    // Botón de PlayStreaming
                    IconButton(onClick = onPlayStreamingClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.streaming),
                            contentDescription = if (isAndainaPlaying) "Detener Streaming" else "Iniciar Streaming",
                            modifier = Modifier.size(32.dp),
                            tint = onPrimaryColor
                        )
                    }
                }
            }

            // Barra de progreso interactiva
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(surfaceVariantColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (!isLiveStream) {
                                    showProgressCircle = true
                                    progressCirclePosition = offset.x / size.width
                                    onProgressChange(progressCirclePosition)
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (!isLiveStream) {
                                    progressCirclePosition = (change.position.x / size.width).coerceIn(0f, 1f)
                                    onProgressChange(progressCirclePosition)
                                }
                            },
                            onDragEnd = {
                                showProgressCircle = false
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progressWidth = size.width * progress
                    drawLine(
                        color = progressColor,
                        start = Offset(0f, center.y),
                        end = Offset(progressWidth, center.y),
                        strokeWidth = size.height
                    )
                    if (showProgressCircle && !isLiveStream) {
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