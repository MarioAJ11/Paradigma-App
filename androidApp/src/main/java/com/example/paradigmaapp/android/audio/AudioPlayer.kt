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
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Composable que representa la interfaz de usuario del reproductor de audio compacto global de la aplicación.
 * Muestra información sobre el episodio actual o el streaming en vivo, y proporciona controles de reproducción.
 *
 * @param activePlayer El reproductor [Player] de Media3 activo. Puede ser nulo.
 * @param currentEpisode El [Episodio] que se está reproduciendo, o `null` si es el stream en vivo.
 * @param andainaRadioInfo Objeto [RadioInfo] con los metadatos del stream en vivo. Puede ser nulo.
 * @param isPlayingGeneral Booleano que indica si algo (episodio o stream) se está reproduciendo.
 * @param episodeProgress Progreso de la reproducción del episodio (un valor flotante entre 0.0 y 1.0).
 * @param onProgressChange Lambda que se invoca cuando el usuario interactúa con la barra de progreso.
 * @param isAndainaStreamActive Indica si el modo de streaming de Andaina está activo.
 * @param isAndainaPlaying Indica si el stream de Andaina se está reproduciendo actualmente.
 * @param onPlayPauseClick Lambda para la acción de reproducir/pausar el contenido actual.
 * @param onPlayStreamingClick Lambda para la acción de (des)activar el modo de streaming.
 * @param onEpisodeInfoClick Lambda que se invoca al hacer clic en la información del episodio para ver sus detalles.
 * @param onVolumeIconClick Lambda para la acción de mostrar el control de volumen.
 * @param modifier Modificador opcional para aplicar al [Card] principal.
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
    // Estado para controlar la visibilidad del círculo de progreso mientras se arrastra.
    var showProgressCircle by remember { mutableStateOf(false) }
    // Estado para la posición del círculo, que puede diferir del progreso real mientras se arrastra.
    var progressCirclePosition by remember { mutableFloatStateOf(episodeProgress) }

    // Efecto que se ejecuta cada vez que cambia el progreso de reproducción.
    // Actualiza la posición del círculo solo si el usuario no lo está arrastrando.
    LaunchedEffect(episodeProgress) {
        if (!showProgressCircle) {
            progressCirclePosition = episodeProgress
        }
    }

    // Colores del tema para la UI del reproductor.
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Determina si el contenido actual es el stream en vivo (si no hay ningún episodio seleccionado).
    val isEffectivelyLiveStream = currentEpisode == null

    // Lógica para determinar qué texto e imagen mostrar.
    val (displayText, displayImageUrl) = if (isEffectivelyLiveStream) {
        val streamImageUrl = andainaRadioInfo?.art

        // Lógica para mostrar si hay o no streaming en directo
        val streamDisplayText = if (isAndainaPlaying || isAndainaStreamActive) {
            "Radio en Directo"
        } else {
            "Sin emisión en directo"
        }
        streamDisplayText to streamImageUrl
        streamDisplayText to streamImageUrl
    } else {
        // Si es un episodio, simplemente mostramos su título e imagen.
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
                // Sección de información (imagen y texto), es clickable si es un episodio.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = currentEpisode != null) { currentEpisode?.let(onEpisodeInfoClick) }
                        .padding(vertical = 4.dp)
                ) {
                    // Carga la imagen desde la URL de forma asíncrona.
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = "Portada de la emisión actual",
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.logo_square),
                        placeholder = painterResource(R.mipmap.logo_square)
                    )
                    Text(
                        text = displayText ?: "Cargando...",
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Sección de botones de control.
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayPauseClick) { Icon(painterResource(if (isPlayingGeneral) R.mipmap.pause else R.mipmap.play), if (isPlayingGeneral) "Pausar" else "Reproducir", Modifier.size(36.dp), tint = onPrimaryColor) }
                    IconButton(onClick = onVolumeIconClick) { Icon(painterResource(R.mipmap.volume), "Control de Volumen", Modifier.size(30.dp), tint = onPrimaryColor) }
                    IconButton(onClick = onPlayStreamingClick) { Icon(painterResource(R.mipmap.streaming), "Radio en Directo", Modifier.size(30.dp), tint = if (isAndainaStreamActive && isAndainaPlaying && isEffectivelyLiveStream) secondaryColor else onPrimaryColor) }
                }
            }

            // Muestra la barra de progreso personalizada.
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