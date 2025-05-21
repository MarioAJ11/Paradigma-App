package com.example.paradigmaapp.android.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.paradigmaapp.android.R
import timber.log.Timber

/**
 * Composable que representa un elemento individual en la lista de podcasts.
 * Muestra la carátula, título y duración del podcast, y permite la selección al hacer clic.
 *
 * @param podcast El [Podcast] que se va a mostrar en la lista.
 * @param onPodcastSelected Lambda que se invoca al hacer clic en este elemento, recibiendo el [Podcast] seleccionado.
 * @param onAddToQueue Lambda que se invoca para añadir el podcast a la cola.
 * @param onRemoveFromQueue Lambda que se invoca para eliminar el podcast de la cola.
 * @param onDownloadPodcast Lambda que se invoca para descargar el podcast.
 * @param onDeleteDownload Lambda que se invoca para eliminar un podcast descargado.
 * @param isDownloaded Booleano que indica si el podcast está descargado.
 * @param isInQueue Booleano que indica si el podcast está en la cola.
 * @param downloadedPodcastIdentifiers Lista de IDs de podcasts descargados (para el estado general de la app).
 * @param queuePodcastUrls Lista de URLs de podcasts en la cola (para el estado general de la app).
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun PodcastListItem(
    podcast: Podcast,
    onPodcastSelected: (Podcast) -> Unit,
    onAddToQueue: (Podcast) -> Unit,
    onRemoveFromQueue: (Podcast) -> Unit,
    onDownloadPodcast: (Podcast, (String) -> Unit) -> Unit,
    onDeleteDownload: (Podcast) -> Unit,
    isDownloaded: Boolean,
    isInQueue: Boolean,
    downloadedPodcastIdentifiers: List<String>,
    queuePodcastUrls: List<String>,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPodcastSelected(podcast) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = "${podcast.title} cover image",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .padding(0.dp),
            contentScale = ContentScale.Crop,
            error = painterResource(R.mipmap.imagen),
            placeholder = painterResource(R.mipmap.imagen)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = podcast.duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Icono de tres puntos y DropdownMenu
        Box(modifier = Modifier.padding(start = 8.dp)) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Opciones del podcast",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Opción Descargar / Eliminar descarga
                if (isDownloaded) {
                    DropdownMenuItem(
                        text = { Text("Eliminar descarga") },
                        onClick = {
                            onDeleteDownload(podcast)
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Descargar") },
                        onClick = {
                            onDownloadPodcast(podcast) { message ->
                                Timber.d("Mensaje de descarga: $message")
                            }
                            showMenu = false
                        }
                    )
                }

                // Opción Añadir a cola / Eliminar de cola
                if (isInQueue) {
                    DropdownMenuItem(
                        text = { Text("Eliminar de cola") },
                        onClick = {
                            onRemoveFromQueue(podcast)
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Añadir a cola") },
                        onClick = {
                            onAddToQueue(podcast)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}