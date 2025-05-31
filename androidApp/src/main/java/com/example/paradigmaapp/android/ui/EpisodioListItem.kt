package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EpisodioListItem(
    episodio: Episodio,
    onPlayEpisode: (Episodio) -> Unit,
    onEpisodeLongClick: (Episodio) -> Unit, // Nueva lambda para pulsación larga
    // onShowOptionsMenu ya no es un parámetro; el menú se maneja internamente por el IconButton
    onAddToQueue: (Episodio) -> Unit,
    onRemoveFromQueue: (Episodio) -> Unit,
    onDownloadEpisode: (Episodio, (String) -> Unit) -> Unit,
    onDeleteDownload: (Episodio) -> Unit,
    isDownloaded: Boolean,
    isInQueue: Boolean,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) } // Renombrado para claridad

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onPlayEpisode(episodio) },
                onLongClick = { onEpisodeLongClick(episodio) } // Acción para pulsación larga
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
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground),
                placeholder = painterResource(R.mipmap.logo_foreground)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = episodio.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis // Cambiado a Ellipsis para mejor visualización
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
                    .padding(end = 0.dp) // Ajustar si es necesario
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
                                onDownloadEpisode(episodio) { message ->
                                    Timber.d("EpisodioListItem: Mensaje de descarga: $message")
                                }
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