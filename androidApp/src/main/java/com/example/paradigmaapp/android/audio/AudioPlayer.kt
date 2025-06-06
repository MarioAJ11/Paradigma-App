package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.RadioInfo
import androidx.compose.ui.graphics.drawscope.Stroke;

/**
 * Composable que representa la interfaz de usuario del reproductor de audio compacto.
 * Muestra información sobre el episodio actual o el streaming en vivo, y proporciona controles de reproducción.
 *
 * @param activePlayer El reproductor [Player] de Media3 activo.
 * @param currentEpisode El [Episodio] que se está reproduciendo, o `null` si es el stream.
 * @param andainaRadioInfo Objeto [RadioInfo] con los metadatos del stream en vivo.
 * @param isPlayingGeneral Booleano que indica si algo se está reproduciendo.
 * @param episodeProgress Progreso de la reproducción del episodio (0.0 a 1.0).
 * @param onProgressChange Lambda que se invoca al interactuar con la barra de progreso.
 * @param isAndainaStreamActive Indica si el modo de streaming de Andaina está activo.
 * @param isAndainaPlaying Indica si el stream de Andaina se está reproduciendo.
 * @param onPlayPauseClick Lambda para reproducir/pausar el contenido actual.
 * @param onPlayStreamingClick Lambda para (des)activar el modo de streaming.
 * @param onEpisodeInfoClick Lambda que se invoca al hacer clic en la información del episodio.
 * @param onVolumeIconClick Lambda para mostrar el control de volumen.
 * @param modifier Modificador opcional para el [Card] principal.
 *
 * @author Mario Alguacil Juárez
 */

@Composable
fun AudioPlayer(
    activePlayer: Player?,
    currentEpisode: Episodio?,
    andainaRadioInfo: RadioInfo?,
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
    var showProgressCircle by remember { mutableStateOf(false) }
    var progressCirclePosition by remember { mutableFloatStateOf(episodeProgress) }

    LaunchedEffect(episodeProgress) {
        if (!showProgressCircle) {
            progressCirclePosition = episodeProgress
        }
    }

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Determina si el contenido actual es el stream en vivo (no hay episodio seleccionado).
    val isEffectivelyLiveStream = currentEpisode == null

    // Determina el texto y la URL de la imagen a mostrar en función de si se reproduce
    // un episodio o el stream en vivo.
    val (displayText, displayImageUrl) = if (isEffectivelyLiveStream) {
        val streamTitle = andainaRadioInfo?.title
        val streamImageUrl = andainaRadioInfo?.art

        val streamDisplayText = when {
            !streamTitle.isNullOrBlank() -> streamTitle // Muestra el título de la canción si está disponible
            andainaRadioInfo != null -> "Radio en Directo" // Si hay conexión pero no título, muestra texto por defecto
            else -> "Sin emisión en directo" // Si no hay conexión/datos, muestra este mensaje
        }
        streamDisplayText to streamImageUrl
    } else {
        // Muestra la información del episodio
        currentEpisode?.title to currentEpisode?.imageUrl
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sección de información (imagen y título)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = currentEpisode != null) { currentEpisode?.let(onEpisodeInfoClick) }
                        .padding(vertical = 4.dp)
                ) {
                    // Muestra dinámicamente la carátula del stream o la del episodio.
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = "Portada de la emisión actual",
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.logo_square),
                        placeholder = painterResource(R.mipmap.logo_square)
                    )

                    // Muestra el título del episodio o la información del stream.
                    Text(
                        text = displayText ?: "Cargando...",
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sección de Controles
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayPauseClick) { Icon(painterResource(if (isPlayingGeneral) R.mipmap.pause else R.mipmap.play), if (isPlayingGeneral) "Pausar" else "Reproducir", Modifier.size(36.dp), tint = onPrimaryColor) }
                    IconButton(onClick = onVolumeIconClick) { Icon(painterResource(R.mipmap.volume), "Control de Volumen", Modifier.size(30.dp), tint = onPrimaryColor) }
                    IconButton(onClick = onPlayStreamingClick) { Icon(painterResource(R.mipmap.streaming), "Radio en Directo", Modifier.size(30.dp), tint = if (isAndainaStreamActive && isAndainaPlaying && isEffectivelyLiveStream) secondaryColor else onPrimaryColor) }
                }
            }

            // Barra de progreso (solo visible e interactiva para episodios)
            AudioProgressBar(
                isLiveStream = isEffectivelyLiveStream,
                progress = episodeProgress,
                circlePosition = progressCirclePosition,
                showCircle = showProgressCircle,
                onProgressChange = onProgressChange,
                onDragStateChange = { isDragging -> showProgressCircle = isDragging }
            )
        }
    }
}

/** Composable auxiliar que encapsula la lógica de la barra de progreso.
 *
 * @param isLiveStream Indica si el contenido es un stream en vivo.
 * @param progress Progreso de la reproducción (0.0 a 1.0).
 * @param circlePosition Posición del círculo de progreso (0.0 a 1.0).
 * @param showCircle Indica si se debe mostrar el círculo de progreso.
 * @param onProgressChange Lambda que se invoca al interactuar con la barra de progreso.
 * @param onDragStateChange Lambda que se invoca cuando el estado de arrastre cambia.
 * @param modifier Modificador opcional para el [Box] principal.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
private fun AudioProgressBar(
    isLiveStream: Boolean,
    progress: Float,
    circlePosition: Float,
    showCircle: Boolean,
    onProgressChange: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLiveStream) {
        // Si es un stream en vivo, no se muestra barra de progreso interactiva.
        Spacer(modifier = modifier.fillMaxWidth().height(8.dp))
        return
    }

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(surfaceVariantColor.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true)
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                    },
                    onDragEnd = { onDragStateChange(false) },
                    onDragCancel = { onDragStateChange(false) }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressWidth = size.width * progress
            drawLine(secondaryColor, start = Offset.Zero.copy(y = center.y), end = Offset(progressWidth, center.y), strokeWidth = size.height)
            if (showCircle) {
                val circleX = (size.width * circlePosition).coerceIn(0f, size.width)
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = 10.dp.toPx(), center = Offset(circleX, center.y))
                drawCircle(onPrimaryColor, radius = 10.dp.toPx(), center = Offset(circleX, center.y), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}