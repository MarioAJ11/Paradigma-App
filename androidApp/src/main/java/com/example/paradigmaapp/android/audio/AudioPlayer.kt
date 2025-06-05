package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Recursos de Android
import com.example.paradigmaapp.model.Episodio // Modelo de Episodio del módulo shared

/**
 * Composable que representa la interfaz de usuario del reproductor de audio compacto.
 * Muestra información sobre el episodio actual o el stream en vivo, y proporciona controles de reproducción.
 * Permite la interacción con la barra de progreso para episodios.
 *
 * @param activePlayer El reproductor [Player] de Media3 activo (puede ser el de podcast o el de stream).
 * Se utiliza para obtener el estado del volumen.
 * @param currentEpisode El [Episodio] que se está reproduciendo actualmente, o `null` si es el stream en vivo.
 * @param isPlayingGeneral Estado booleano que indica si algo (episodio o stream) se está reproduciendo actualmente.
 * @param episodeProgress Progreso de la reproducción del episodio actual, como un [Float] entre 0.0 y 1.0.
 * @param onProgressChange Lambda que se invoca cuando el usuario interactúa con la barra de progreso.
 * Recibe el nuevo progreso como un [Float].
 * @param isAndainaStreamActive Indica si el modo de streaming de Andaina FM ha sido seleccionado por el usuario.
 * @param isAndainaPlaying Indica si el stream de Andaina FM se está reproduciendo específicamente.
 * @param onPlayPauseClick Lambda para ser invocada al pulsar el botón de reproducir/pausar.
 * @param onPlayStreamingClick Lambda para ser invocada al pulsar el botón de (des)activar el streaming de Andaina FM.
 * @param onEpisodeInfoClick Lambda que se invoca al hacer clic en la información del episodio (imagen o título).
 * Recibe el [Episodio] actual.
 * @param onVolumeIconClick Lambda para ser invocada al pulsar el icono de volumen, usualmente para mostrar un control de volumen más detallado.
 * @param modifier Modificador opcional para personalizar el contenedor principal [Card].
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun AudioPlayer(
    activePlayer: Player?,
    currentEpisode: Episodio?,
    isPlayingGeneral: Boolean,
    episodeProgress: Float,
    onProgressChange: (Float) -> Unit,
    isAndainaStreamActive: Boolean,
    isAndainaPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPlayStreamingClick: () -> Unit,
    onEpisodeInfoClick: (Episodio) -> Unit,
    onVolumeIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No se usa currentVolume aquí, se asume que el icono de volumen solo abre el control detallado.
    // var currentVolume by remember { mutableFloatStateOf(activePlayer?.volume ?: 0f) }
    // LaunchedEffect(activePlayer?.volume) {
    // currentVolume = activePlayer?.volume ?: 0f
    // }

    var showProgressCircle by remember { mutableStateOf(false) }
    var progressCirclePosition by remember { mutableFloatStateOf(episodeProgress) }

    LaunchedEffect(episodeProgress) {
        if (!showProgressCircle) {
            progressCirclePosition = episodeProgress
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val isEffectivelyLiveStream = currentEpisode == null

    Card(
        modifier = modifier
            .fillMaxWidth()
            // .background(Color.Transparent) // El Card ya tiene su propio fondo, este es redundante.
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = currentEpisode != null) {
                            currentEpisode?.let { onEpisodeInfoClick(it) }
                        }
                        .padding(vertical = 4.dp) // Padding para aumentar área táctil de la información
                ) {
                    if (currentEpisode?.imageUrl != null) {
                        AsyncImage(
                            model = currentEpisode.imageUrl,
                            contentDescription = "Portada de ${currentEpisode.title}", //
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground),
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.mipmap.logo_foreground),
                                contentDescription = "Radio en vivo", //
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = currentEpisode?.title ?: "Radio en Directo", //
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            painter = painterResource(id = if (isPlayingGeneral) R.mipmap.pause else R.mipmap.play), //
                            contentDescription = if (isPlayingGeneral) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp),
                            tint = onPrimaryColor
                        )
                    }
                    IconButton(onClick = onVolumeIconClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.volume), //
                            contentDescription = "Control de Volumen",
                            modifier = Modifier.size(30.dp),
                            tint = onPrimaryColor
                        )
                    }
                    IconButton(onClick = onPlayStreamingClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.streaming), //
                            contentDescription = if (isAndainaStreamActive) "Detener Radio en Directo" else "Escuchar Radio en Directo",
                            modifier = Modifier.size(30.dp),
                            tint = if (isAndainaStreamActive && isAndainaPlaying && currentEpisode == null) secondaryColor else onPrimaryColor
                        )
                    }
                }
            }

            val progressModifier = if (isEffectivelyLiveStream) {
                Modifier
                    .fillMaxWidth()
                    .height(4.dp) // Altura más pequeña si no es interactiva
                    .background(Color.Transparent) // Sin fondo visible si es live stream
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(surfaceVariantColor.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                showProgressCircle = true
                                val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                progressCirclePosition = newProgress
                                onProgressChange(newProgress)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                progressCirclePosition = newProgress
                                onProgressChange(newProgress)
                            },
                            onDragEnd = {
                                showProgressCircle = false
                                // onProgressChange ya se llamó, el reproductor buscará la posición.
                            },
                            onDragCancel = {
                                showProgressCircle = false
                            }
                        )
                    }
            }

            Box(modifier = progressModifier) {
                if (!isEffectivelyLiveStream) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val progressWidth = size.width * episodeProgress
                        drawLine(
                            color = secondaryColor,
                            start = Offset(0f, center.y),
                            end = Offset(progressWidth, center.y),
                            strokeWidth = size.height
                        )
                        if (showProgressCircle) {
                            val circleX = (size.width * progressCirclePosition).coerceIn(0f, size.width)
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.7f),
                                radius = 10.dp.toPx(),
                                center = Offset(circleX, center.y)
                            )
                            drawCircle(
                                color = onPrimaryColor,
                                radius = 10.dp.toPx(),
                                center = Offset(circleX, center.y),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}