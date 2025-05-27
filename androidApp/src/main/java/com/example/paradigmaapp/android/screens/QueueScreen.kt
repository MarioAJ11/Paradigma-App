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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem // Importa el item de episodio actualizado
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit, // Acción de reproducir
    onBackClick: () -> Unit
) {
    val queueEpisodios by queueViewModel.queueEpisodios.collectAsState()
    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cola de Reproducción",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                            onPlayEpisode = { onEpisodeSelected(it) }, // Acción de reproducir
                            onAddToQueue = { /* Ya está en cola, no se necesita esta acción específica aquí o podría ser para reordenar */ },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { ep, onMsgCallback ->
                                downloadedViewModel.downloadEpisodio(ep) { message ->
                                    onMsgCallback(message)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            },
                            onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                            isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                            isInQueue = true, // Todos los items aquí están en cola
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}