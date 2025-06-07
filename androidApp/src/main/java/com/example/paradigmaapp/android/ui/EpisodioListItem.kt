package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * Un Composable que muestra un ítem individual de [Episodio] en una lista.
 * Proporciona información básica del episodio como imagen y título.
 * Incluye interactividad para reproducir el episodio, una acción de pulsación larga,
 * y un menú contextual con opciones adicionales.
 * La imagen se muestra sin padding y el título se muestra completo.
 *
 * @param episodio El objeto [Episodio] a mostrar.
 * @param onPlayEpisode Lambda que se invoca cuando el usuario hace clic en el ítem para reproducir el episodio.
 * @param onEpisodeLongClick Lambda que se invoca cuando el usuario realiza una pulsación larga sobre el ítem.
 * @param onAddToQueue Lambda para añadir el episodio a la cola de reproducción.
 * @param onRemoveFromQueue Lambda para eliminar el episodio de la cola de reproducción.
 * @param onDownloadEpisode Lambda para iniciar la descarga del episodio.
 * @param onDeleteDownload Lambda para eliminar un episodio descargado.
 * @param isDownloaded Booleano que indica si el episodio está actualmente descargado.
 * @param isInQueue Booleano que indica si el episodio está actualmente en la cola de reproducción.
 * @param modifier Modificador opcional para aplicar al [Card] principal.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EpisodioListItem(
    episodio: Episodio,
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
            AsyncImage(
                model = episodio.imageUrl,
                contentDescription = "Portada de ${episodio.title}",
                modifier = Modifier
                    .size(72.dp) // Aumentamos un poco el tamaño de la imagen para compensar la falta de padding visual
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)), // Redondea esquinas izquierdas para que coincida con la Card
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground),
                placeholder = painterResource(R.mipmap.logo_foreground)
            )

            // El Spacer y la Column ahora tendrán un padding inicial para separarlos de la imagen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp), // Padding para el contenido de texto
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

            // IconButton para mostrar el menú de opciones rápidas
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp) // Padding ligero para el botón de menú
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
                        DropdownMenuItem(
                            text = { Text("Eliminar descarga") },
                            onClick = {
                                onDeleteDownload(episodio)
                                showContextMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Descargar") },
                            onClick = {
                                onDownloadEpisode(episodio) { /* Mensaje gestionado por el llamador */ }
                                showContextMenu = false
                            }
                        )
                    }

                    if (isInQueue) {
                        DropdownMenuItem(
                            text = { Text("Eliminar de cola") },
                            onClick = {
                                onRemoveFromQueue(episodio)
                                showContextMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Añadir a cola") },
                            onClick = {
                                onAddToQueue(episodio)
                                showContextMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}