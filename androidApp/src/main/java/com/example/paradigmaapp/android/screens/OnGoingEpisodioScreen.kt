package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.OnGoingEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio

/**
 * Pantalla que muestra los episodios cuya reproducción está actualmente en curso o pausada.
 * Permite al usuario continuar escuchando desde donde lo dejó.
 *
 * @param onGoingEpisodioViewModel ViewModel que gestiona la lógica y el estado de los episodios en curso.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción (necesario para EpisodioListItem).
 * @param downloadedViewModel ViewModel para interactuar con las descargas (necesario para EpisodioListItem).
 * @param onEpisodeSelected Lambda que se invoca cuando un episodio es seleccionado para reanudar su reproducción.
 * @param onBackClick Lambda para manejar la acción de retroceso y cerrar esta pantalla.
 * @author Mario Alguacil Juárez
 */
@Composable
fun OnGoingEpisodioScreen(
    onGoingEpisodioViewModel: OnGoingEpisodioViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa la lista de episodios en curso desde el ViewModel correspondiente.
    val onGoingEpisodios by onGoingEpisodioViewModel.onGoingEpisodios.collectAsState()

    // Observa los IDs de los episodios en cola y descargados para el estado en EpisodioListItem.
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()
    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fondo de la pantalla
    ) {
        // Barra superior con botón de retroceso.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre el icono y el título
            Text(
                text = "Seguir Escuchando", // Título de la pantalla
                style = MaterialTheme.typography.titleLarge, // Estilo del título
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Color del texto
            )
        }

        // Contenido principal de la pantalla.
        if (onGoingEpisodios.isEmpty()) {
            // Mensaje centrado si no hay episodios en progreso.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Padding alrededor del texto
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes episodios en progreso para continuar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center // Texto centrado
                )
            }
        } else {
            // Lista scrollable de episodios en progreso.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp) // Padding vertical para la lista
            ) {
                items(onGoingEpisodios, key = { episodio -> episodio.id }) { episodio ->
                    // Cada item de la lista es un EpisodioListItem.
                    // Se le pasan todas las dependencias y callbacks necesarios.
                    EpisodioListItem(
                        episodio = episodio,
                        onEpisodeSelected = { onEpisodeSelected(episodio) }, // Acción al seleccionar el episodio
                        onAddToQueue = { queueViewModel.addEpisodeToQueue(episodio) },
                        onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(episodio) },
                        onDownloadEpisode = { ep, onMsgCallback ->
                            downloadedViewModel.downloadEpisodio(ep, onMsgCallback)
                        },
                        onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(episodio) },
                        isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                        isInQueue = queueEpisodeIds.contains(episodio.id)
                        // Considera pasar y mostrar el progreso del episodio si tu
                        // OnGoingEpisodioViewModel provee esta información de forma detallada
                        // y EpisodioListItem está preparado para mostrar una barra de progreso.
                    )
                }
            }
        }
    }
}