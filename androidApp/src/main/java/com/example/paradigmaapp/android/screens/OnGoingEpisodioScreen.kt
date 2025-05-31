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
import com.example.paradigmaapp.android.viewmodel.OnGoingEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnGoingEpisodioScreen(
    onGoingEpisodioViewModel: OnGoingEpisodioViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa los estados desde los ViewModels
    val onGoingEpisodios by onGoingEpisodioViewModel.onGoingEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()
    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()

    // Para el Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Seguir Escuchando",
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
            if (onGoingEpisodios.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes episodios en progreso para continuar.",
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
                    items(onGoingEpisodios, key = { episodio -> episodio.id }) { episodio ->
                        EpisodioListItem(
                            episodio = episodio,
                            onPlayEpisode = { onEpisodeSelected(it) },
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
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
                            isInQueue = queueEpisodeIds.contains(episodio.id),
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}