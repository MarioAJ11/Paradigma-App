package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.model.Episodio

/**
 * Muestra un ítem de episodio individual en una lista.
 * Proporciona feedback visual con un indicador de carga cuando el episodio se está preparando
 * para la reproducción. También incluye un menú contextual para acciones adicionales.
 *
 * @param episodio El objeto [Episodio] cuyos datos se van a mostrar.
 * @param isLoading Booleano que indica si se debe mostrar el indicador de carga.
 * @param onPlayEpisode Lambda que se invoca al hacer clic para reproducir el episodio.
 * @param onEpisodeLongClick Lambda que se invoca con una pulsación larga sobre el ítem.
 * @param onAddToQueue Lambda para añadir el episodio a la cola.
 * @param onRemoveFromQueue Lambda para quitar el episodio de la cola.
 * @param onDownloadEpisode Lambda para iniciar la descarga del episodio.
 * @param onDeleteDownload Lambda para eliminar una descarga existente.
 * @param isDownloaded Booleano que indica si el episodio ya está descargado.
 * @param isInQueue Booleano que indica si el episodio ya está en la cola.
 * @param modifier Modificador de Compose para personalizar el estilo del componente.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodioListItem(
    episodio: Episodio,
    isLoading: Boolean,
    onPlayEpisode: (Episodio) -> Unit,
    onEpisodeLongClick: (Episodio) -> Unit,
    onAddToQueue: (Episodio) -> Unit,
    onRemoveFromQueue: (Episodio) -> Unit,
    onDownloadEpisode: (Episodio, (String) -> Unit) -> Unit,
    onDeleteDownload: (Episodio) -> Unit,
    isDownloaded: Boolean,
    isInQueue: Boolean,
    modifier: Modifier = Modifier
) {
    // Estado para controlar la visibilidad del menú de opciones.
    var showContextMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onPlayEpisode(episodio) },
                onLongClick = { onEpisodeLongClick(episodio) }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor para la imagen o el indicador de carga.
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    // Muestra un spinner si el episodio se está cargando.
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Muestra la imagen del episodio si no está cargando.
                    AsyncImage(
                        model = episodio.imageUrl,
                        contentDescription = "Portada de ${episodio.title}",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.logo_foreground),
                        placeholder = painterResource(R.mipmap.logo_foreground)
                    )
                }
            }

            // Columna con el título y la duración del episodio.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = episodio.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (episodio.duration.isNotBlank() && episodio.duration != "--:--") {
                    Text(
                        text = episodio.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            // Botón y menú de opciones (descargar, añadir a cola, etc.).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp)
            ) {
                IconButton(onClick = { showContextMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Opciones para ${episodio.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    if (isDownloaded) {
                        DropdownMenuItem(text = { Text("Eliminar descarga") }, onClick = { onDeleteDownload(episodio); showContextMenu = false })
                    } else {
                        DropdownMenuItem(text = { Text("Descargar") }, onClick = { onDownloadEpisode(episodio) { /* msg */ }; showContextMenu = false })
                    }

                    if (isInQueue) {
                        DropdownMenuItem(text = { Text("Eliminar de cola") }, onClick = { onRemoveFromQueue(episodio); showContextMenu = false })
                    } else {
                        DropdownMenuItem(text = { Text("Añadir a cola") }, onClick = { onAddToQueue(episodio); showContextMenu = false })
                    }
                }
            }
        }
    }
}