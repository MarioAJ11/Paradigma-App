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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Asegúrate que R se importa correctamente
import com.example.paradigmaapp.model.Episodio // El modelo de Episodio del módulo shared
import timber.log.Timber

/**
 * Composable que representa un elemento individual en una lista de episodios.
 * Muestra la carátula, título y duración del episodio.
 * Permite reproducir con un clic y mostrar opciones con una pulsación larga o clic en el icono de menú.
 *
 * @param episodio El [Episodio] que se va a mostrar.
 * @param onPlayEpisode Lambda que se invoca con un clic normal para reproducir el episodio.
 * @param onAddToQueue Lambda para añadir el episodio a la cola (usado por el menú).
 * @param onRemoveFromQueue Lambda para eliminar el episodio de la cola (usado por el menú).
 * @param onDownloadEpisode Lambda para descargar el episodio (usado por el menú). Recibe el episodio y un callback para mensajes.
 * @param onDeleteDownload Lambda para eliminar un episodio descargado (usado por el menú).
 * @param isDownloaded Booleano que indica si el episodio está descargado.
 * @param isInQueue Booleano que indica si el episodio está en la cola.
 * @param modifier Modificador opcional para aplicar a este Composable.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class) // ExperimentalMaterial3Api para componentes como Card
@Composable
fun EpisodioListItem(
    episodio: Episodio,
    onPlayEpisode: (Episodio) -> Unit, // Para el clic normal (reproducir)
    // onShowOptionsMenu ya no es un parámetro; el menú se maneja internamente
    onAddToQueue: (Episodio) -> Unit,
    onRemoveFromQueue: (Episodio) -> Unit,
    onDownloadEpisode: (Episodio, (String) -> Unit) -> Unit,
    onDeleteDownload: (Episodio) -> Unit,
    isDownloaded: Boolean,
    isInQueue: Boolean,
    modifier: Modifier = Modifier
) {
    // Estado para controlar la visibilidad del menú desplegable interno
    var showMenuState by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onPlayEpisode(episodio) },
                onLongClick = { showMenuState = true }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Alinea verticalmente los elementos del Row
        ) {
            // Imagen del episodio
            AsyncImage(
                model = episodio.imageUrl, // URL de la imagen del episodio
                contentDescription = "Portada de ${episodio.title}", // Descripción para accesibilidad
                modifier = Modifier
                    .padding(0.dp) // Padding a la izquierda de la imagen para separarla del borde de la Card
                    .size(64.dp) // Tamaño de la imagen
                    .clip(RoundedCornerShape(8.dp)), // Esquinas redondeadas para la propia imagen
                contentScale = ContentScale.Crop, // Cómo se escala la imagen para llenar el tamaño
                error = painterResource(R.mipmap.logo_foreground), // Imagen a mostrar en caso de error de carga
                placeholder = painterResource(R.mipmap.logo_foreground) // Imagen a mostrar mientras carga la real
            )

            // Espacio horizontal entre la imagen y la columna de texto
            Spacer(modifier = Modifier.width(16.dp))

            // Columna para el Título y la Duración del episodio
            Column(
                modifier = Modifier.weight(1f)
                    .padding(vertical = 10.dp), // La columna toma el espacio restante horizontalmente
                verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio vertical entre el título y la duración
            ) {
                Text(
                    text = episodio.title, // Título del episodio
                    style = MaterialTheme.typography.titleMedium, // Estilo del texto del título
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Color del texto del título
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
                // Muestra la duración si está disponible y no es el placeholder
                if (episodio.duration.isNotBlank() && episodio.duration != "--:--") {
                    Text(
                        text = episodio.duration, // Duración del episodio
                        style = MaterialTheme.typography.bodySmall, // Estilo del texto de la duración
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // Color más tenue para la duración
                        maxLines = 1 // Máximo una línea para la duración
                    )
                }
            }

            // Contenedor para el icono de menú y el menú desplegable
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically) // Alinea el Box verticalmente dentro del Row
                    // Padding a la derecha del icono para separarlo del borde de la Card
                    .padding(end = 8.dp)
            ) {
                // IconButton para mostrar el menú de opciones
                IconButton(onClick = { showMenuState = true }) { // Al hacer clic en el icono, se muestra el menú
                    Icon(
                        Icons.Filled.MoreVert, // Icono estándar de "más opciones"
                        contentDescription = "Opciones para ${episodio.title}", // Descripción para accesibilidad
                        tint = MaterialTheme.colorScheme.onSurfaceVariant // Color del icono
                    )
                }
                // Menú desplegable de opciones
                DropdownMenu(
                    expanded = showMenuState, // Su visibilidad depende del estado showMenuState
                    onDismissRequest = { showMenuState = false } // Acción para cerrar el menú (clic fuera)
                ) {
                    // Opción para Descargar o Eliminar descarga
                    if (isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("Eliminar descarga") },
                            onClick = {
                                onDeleteDownload(episodio) // Llama a la acción de eliminar descarga
                                showMenuState = false // Cierra el menú
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Descargar") },
                            onClick = {
                                onDownloadEpisode(episodio) { message -> // Llama a la acción de descargar
                                    // El mensaje de feedback (ej. Snackbar) se debe manejar en la pantalla contenedora
                                    Timber.d("EpisodioListItem: Mensaje de descarga: $message")
                                }
                                showMenuState = false // Cierra el menú
                            }
                        )
                    }

                    // Opción para Añadir a cola o Eliminar de cola
                    if (isInQueue) {
                        DropdownMenuItem(
                            text = { Text("Eliminar de cola") },
                            onClick = {
                                onRemoveFromQueue(episodio) // Llama a la acción de eliminar de la cola
                                showMenuState = false // Cierra el menú
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Añadir a cola") },
                            onClick = {
                                onAddToQueue(episodio) // Llama a la acción de añadir a la cola
                                showMenuState = false // Cierra el menú
                            }
                        )
                    }
                }
            }
        }
    }
}