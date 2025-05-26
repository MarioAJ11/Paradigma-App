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
import com.example.paradigmaapp.android.ui.EpisodioListItem // Usar el nuevo item
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio

/**
 * Pantalla que muestra la cola de reproducción.
 *
 * @param queueViewModel ViewModel para la cola.
 * @param downloadedViewModel ViewModel para el estado de descargas (para EpisodioListItem).
 * @param onEpisodeSelected Lambda para cuando un episodio es seleccionado para reproducción.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    val queueEpisodios by queueViewModel.queueEpisodios.collectAsState()
    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()

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
                text = "Cola de Reproducción",
                style = MaterialTheme.typography.titleLarge, // Un título más grande
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (queueEpisodios.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tu cola de reproducción está vacía.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(queueEpisodios, key = { it.id }) { episodio ->
                    EpisodioListItem(
                        episodio = episodio,
                        onEpisodeSelected = onEpisodeSelected,
                        onAddToQueue = { /* Ya está en la cola, quizá reordenar o no hacer nada */ },
                        onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                        onDownloadEpisode = { ep, onMsg ->
                            downloadedViewModel.downloadEpisodio(
                                ep,
                                onMsg
                            )
                        },
                        onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                        isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                        isInQueue = true,
                        modifier = TODO(),
                        onEpisodeClicked = TODO() // Todos los items aquí están en cola
                    )
                }
            }
        }
    }
}