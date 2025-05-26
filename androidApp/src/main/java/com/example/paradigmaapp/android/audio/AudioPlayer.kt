package com.example.paradigmaapp.android.audio;

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.media3.common.Player // Asegúrate que es este Player de Media3
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Tus recursos de Android
import com.example.paradigmaapp.model.Episodio // El nuevo modelo de Episodio del módulo shared

/**
 * Composable que representa la interfaz de un reproductor de audio compacto.
 * Muestra información sobre el episodio actual o el streaming en vivo, y proporciona controles de reproducción.
 *
 * @param activePlayer El reproductor {@link Player} activo (puede ser el de podcast o el de stream).
 * @param currentEpisode El [Episodio] que se está reproduciendo actualmente, o null si es el stream en vivo.
 * @param isPlayingGeneral Estado booleano que indica si algo (episodio o stream) se está reproduciendo actualmente.
 * @param episodeProgress Progreso de la reproducción del episodio actual (0.0 a 1.0).
 * @param onProgressChange Lambda que se invoca cuando el usuario interactúa con la barra de progreso del episodio.
 * @param isAndainaStreamActive Indica si el modo de streaming de Andaina está activo (seleccionado por el usuario).
 * @param isAndainaPlaying Indica si el stream de Andaina se está reproduciendo actualmente.
 * @param onPlayPauseClick Lambda para reproducir/pausar el contenido actual (episodio o stream).
 * @param onPlayStreamingClick Lambda para (des)activar el modo de streaming de Andaina.
 * @param onEpisodeInfoClick Lambda que se invoca al hacer clic en la información del episodio.
 * @param onVolumeIconClick Lambda para mostrar el control de volumen detallado.
 * @param modifier Modificador opcional para personalizar el diseño.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun AudioPlayer(
    activePlayer: Player?, // El reproductor que está actualmente en uso (podcast o Andaina)
    currentEpisode: Episodio?,
    isPlayingGeneral: Boolean, // Indica si CUALQUIER COSA (episodio o stream) está sonando
    episodeProgress: Float,
    onProgressChange: (Float) -> Unit,
    isAndainaStreamActive: Boolean, // Si el usuario ha seleccionado el modo "Radio en vivo"
    isAndainaPlaying: Boolean, // Si el stream de Andaina está específicamente sonando
    onPlayPauseClick: () -> Unit,
    onPlayStreamingClick: () -> Unit,
    onEpisodeInfoClick: (Episodio) -> Unit, // Ahora recibe el Episodio
    onVolumeIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado local para el volumen, se actualiza si el volumen del `activePlayer` cambia.
    var currentVolume by remember { mutableFloatStateOf(activePlayer?.volume ?: 0f) }
    LaunchedEffect(activePlayer?.volume) {
        currentVolume = activePlayer?.volume ?: 0f
    }

    // Estados para la interacción con la barra de progreso.
    var showProgressCircle by remember { mutableStateOf(false) }
    var progressCirclePosition by remember { mutableFloatStateOf(episodeProgress) } // Inicializar con el progreso actual

    // Actualizar progressCirclePosition si episodeProgress cambia externamente (ej. por reproducción normal)
    // y el usuario no está arrastrando.
    LaunchedEffect(episodeProgress) {
        if (!showProgressCircle) {
            progressCirclePosition = episodeProgress
        }
    }

    // Define los colores del tema para usarlos fácilmente.
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary // Para la barra de progreso
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant // Para el fondo de la barra

    // Determina si el contenido actual es un stream en vivo (no hay episodio seleccionado).
    val isEffectivelyLiveStream = currentEpisode == null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp), // Espaciado del reproductor
        colors = CardDefaults.cardColors(containerColor = primaryColor),
        shape = RoundedCornerShape(12.dp) // Esquinas un poco más redondeadas
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding interno
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre la fila de info/controles y la barra
        ) {
            // Fila superior: Información del episodio/stream y controles principales.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sección de información (imagen y título).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp), // Espacio entre imagen y texto
                    modifier = Modifier
                        .weight(1f) // Permite que esta sección crezca y empuje los controles
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = currentEpisode != null) {
                            currentEpisode?.let { onEpisodeInfoClick(it) }
                        }
                        .padding(vertical = 4.dp) // Padding para el área clickeable
                ) {
                    // Imagen: Muestra la imagen del episodio o un placeholder para el stream.
                    if (currentEpisode?.imageUrl != null) {
                        AsyncImage(
                            model = currentEpisode.imageUrl,
                            contentDescription = "Portada de ${currentEpisode.title}",
                            modifier = Modifier
                                .size(56.dp) // Tamaño de la imagen
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground), // Placeholder desde tus recursos
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )
                    } else {
                        // Placeholder para cuando es stream en vivo o no hay imagen.
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer), // Un fondo sutil
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.mipmap.logo_foreground), // O un icono específico de radio
                                contentDescription = "Radio en vivo",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Texto: Título del episodio o "Radio en Directo".
                    Text(
                        text = currentEpisode?.title ?: "Radio en Directo",
                        style = MaterialTheme.typography.titleSmall, // Un poco más pequeño para que quepa mejor
                        color = onPrimaryColor,
                        maxLines = 2, // Permitir hasta dos líneas
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sección de Controles (Play/Pause, Volumen, Stream).
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Play/Pause.
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            painter = painterResource(id = if (isPlayingGeneral) R.mipmap.pause else R.mipmap.play),
                            contentDescription = if (isPlayingGeneral) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp), // Icono un poco más grande
                            tint = onPrimaryColor
                        )
                    }
                    // Botón de Volumen.
                    IconButton(onClick = onVolumeIconClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.volume),
                            contentDescription = "Control de Volumen",
                            modifier = Modifier.size(30.dp),
                            tint = onPrimaryColor
                        )
                    }
                    // Botón para (Des)Activar el Streaming de Andaina.
                    IconButton(onClick = onPlayStreamingClick) {
                        Icon(
                            painter = painterResource(id = R.mipmap.streaming),
                            contentDescription = if (isAndainaStreamActive) "Detener Radio en Directo" else "Escuchar Radio en Directo",
                            modifier = Modifier.size(30.dp),
                            // Cambia el tinte si el stream de Andaina está activo Y sonando
                            tint = if (isAndainaStreamActive && isAndainaPlaying && currentEpisode == null) secondaryColor else onPrimaryColor
                        )
                    }
                }
            }

            // Barra de progreso: Solo visible e interactiva para episodios.
            val progressModifier = if (isEffectivelyLiveStream) {
                Modifier // Sin interacción si es live stream
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Transparent) // Fondo transparente si es live
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(8.dp) // Un poco más gruesa para facilitar la interacción
                    .clip(RoundedCornerShape(4.dp)) // Bordes redondeados para la barra
                    .background(surfaceVariantColor.copy(alpha = 0.3f)) // Fondo de la barra con alfa
                    .pointerInput(Unit) { // Clave Unit para que el listener no se recomponga innecesariamente
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                showProgressCircle = true
                                val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                progressCirclePosition = newProgress
                                onProgressChange(newProgress) // Actualiza el progreso inmediatamente
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume() // Consumir el evento de arrastre
                                val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                progressCirclePosition = newProgress
                                onProgressChange(newProgress) // Actualiza el progreso mientras se arrastra
                            },
                            onDragEnd = {
                                showProgressCircle = false
                                // onProgressChange ya se llamó en onHorizontalDrag, el reproductor debería buscar.
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
                        // Dibuja la parte de progreso de la barra.
                        val progressWidth = size.width * episodeProgress // Usa el progreso real del episodio
                        drawLine(
                            color = secondaryColor,
                            start = Offset(0f, center.y),
                            end = Offset(progressWidth, center.y),
                            strokeWidth = size.height
                        )
                        // Dibuja el círculo de arrastre si está visible.
                        if (showProgressCircle) {
                            val circleX = (size.width * progressCirclePosition).coerceIn(0f, size.width)
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.7f), // Círculo semi-transparente
                                radius = 10.dp.toPx(), // Círculo un poco más grande
                                center = Offset(circleX, center.y)
                            )
                            drawCircle( // Borde del círculo
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