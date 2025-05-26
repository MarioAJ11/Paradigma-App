package com.example.paradigmaapp.android.ui // O donde decidas ubicarlo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Asegúrate que R se importa correctamente
import com.example.paradigmaapp.model.Episodio
import timber.log.Timber

/**
 * Composable que representa un elemento individual en una lista de episodios.
 * Muestra la carátula, título y duración del episodio, y permite acciones
 * como añadir a la cola, descargar, etc., a través de un menú.
 *
 * @param episodio El [Episodio] que se va a mostrar.
 * @param onEpisodeSelected Lambda que se invoca al hacer clic en este elemento.
 * @param onAddToQueue Lambda para añadir el episodio a la cola.
 * @param onRemoveFromQueue Lambda para eliminar el episodio de la cola.
 * @param onDownloadEpisode Lambda para descargar el episodio. Recibe el episodio y un callback para mensajes.
 * @param onDeleteDownload Lambda para eliminar un episodio descargado.
 * @param isDownloaded Booleano que indica si el episodio está descargado.
 * @param isInQueue Booleano que indica si el episodio está en la cola.
 * @param modifier Modificador opcional para aplicar a este Composable.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun EpisodioListItem(
    episodio: Episodio,
    onEpisodeSelected: (Episodio) -> Unit,
    onAddToQueue: (Episodio) -> Unit,
    onRemoveFromQueue: (Episodio) -> Unit,
    onDownloadEpisode: (Episodio, (String) -> Unit) -> Unit, // (Episodio, MensajeCallback)
    onDeleteDownload: (Episodio) -> Unit,
    isDownloaded: Boolean,
    isInQueue: Boolean,
    modifier: Modifier = Modifier,
    onEpisodeClicked: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current // Para el Snackbar u otro feedback

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEpisodeSelected(episodio) }
            .padding(horizontal = 16.dp, vertical = 10.dp), // Un poco más de padding vertical
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen del episodio
        AsyncImage(
            model = episodio.imageUrl,
            contentDescription = "Portada de ${episodio.title}",
            modifier = Modifier
                .size(64.dp) // Tamaño consistente
                .clip(RoundedCornerShape(8.dp)), // Esquinas redondeadas
            contentScale = ContentScale.Crop,
            error = painterResource(R.mipmap.logo_foreground), // Placeholder/Error desde tus resources
            placeholder = painterResource(R.mipmap.logo_foreground)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Columna para Título y Duración
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre título y duración
        ) {
            Text(
                text = episodio.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, // Permitir hasta 2 líneas para el título
                overflow = TextOverflow.Ellipsis
            )
            // Si la duración no está disponible, no la mostramos o mostramos un placeholder
            if (episodio.duration.isNotBlank() && episodio.duration != "--:--") {
                Text(
                    text = episodio.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Icono de tres puntos para el menú de opciones
        Box(modifier = Modifier.align(Alignment.CenterVertically)) { // Alinear verticalmente el icono
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Opciones para ${episodio.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Menú desplegable
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Opción Descargar / Eliminar descarga
                if (isDownloaded) {
                    DropdownMenuItem(
                        text = { Text("Eliminar descarga") },
                        onClick = {
                            onDeleteDownload(episodio)
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Descargar") },
                        onClick = {
                            onDownloadEpisode(episodio) { message ->
                                // TODO:Aquí puedes usar el 'message' para un Snackbar, Toast, etc.
                                // scope.launch { snackbarHostState.showSnackbar(message) }
                                Timber.d("Acción descarga: $message")
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
                            onRemoveFromQueue(episodio)
                            showMenu = false
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Añadir a cola") },
                        onClick = {
                            onAddToQueue(episodio)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}