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
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio

/**
 * Pantalla que muestra los episodios que han sido descargados por el usuario.
 *
 * @param downloadedEpisodioViewModel ViewModel para los episodios descargados.
 * @param queueViewModel ViewModel para gestionar acciones de la cola.
 * @param onEpisodeSelected Lambda que se invoca cuando un episodio es seleccionado para reproducción.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@Composable
fun DownloadedEpisodioScreen(
    downloadedEpisodioViewModel: DownloadedEpisodioViewModel,
    queueViewModel: QueueViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa la lista de episodios descargados desde el ViewModel.
    val downloadedEpisodios by downloadedEpisodioViewModel.downloadedEpisodios.collectAsState()
    // Observa los IDs de los episodios en cola para el estado en EpisodioListItem.
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mis Descargas", // Título de la pantalla
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Contenido de la pantalla.
        if (downloadedEpisodios.isEmpty()) {
            // Mensaje si no hay episodios descargados.
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes episodios descargados.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Lista de episodios descargados.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(downloadedEpisodios, key = { it.id }) { episodio ->
                    // Para cada episodio descargado, se usa el Composable EpisodioListItem.
                    EpisodioListItem(
                        episodio = episodio,
                        onEpisodeSelected = { onEpisodeSelected(episodio) }, // Selección para reproducir
                        onAddToQueue = { queueViewModel.addEpisodeToQueue(episodio) },
                        onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(episodio) },
                        // La acción de descargar no es aplicable aquí, ya están descargados.
                        // Se podría tener un callback `onPlayDownloaded` si la lógica de reproducción es diferente.
                        onDownloadEpisode = { _, onMsg -> onMsg("Este episodio ya está descargado.") },
                        onDeleteDownload = { downloadedEpisodioViewModel.deleteDownloadedEpisodio(episodio) }, // Permitir eliminar descarga
                        isDownloaded = true, // Todos los items aquí están descargados
                        isInQueue = queueEpisodeIds.contains(episodio.id)
                    )
                }
            }
        }
    }
}