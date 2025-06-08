package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Muestra la cola de reproducción actual del usuario, permitiéndole ver y gestionar
 * los próximos episodios a escuchar.
 *
 * @param queueViewModel ViewModel que gestiona la cola de reproducción.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param downloadedViewModel ViewModel para interactuar con las descargas.
 * @param onEpisodeSelected Lambda que se invoca al seleccionar un episodio.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un episodio.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel,
    mainViewModel: MainViewModel, // <-- PARÁMETRO AÑADIDO
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Estados de la pantalla
    val queueEpisodios by queueViewModel.queueEpisodios.collectAsState()
    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState() // <-- AÑADIDO

    // Controladores de UI y corutinas
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cola de Reproducción", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)
        ) {
            if (queueEpisodios.isEmpty()) {
                // Mensaje si la cola está vacía.
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text("Tu cola de reproducción está vacía.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            } else {
                // Lista de episodios en la cola.
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)) {
                    items(queueEpisodios, key = { it.id }) { episodio ->
                        val isLoading = episodio.id == preparingEpisodeId
                        EpisodioListItem(
                            episodio = episodio,
                            isLoading = isLoading,
                            onPlayEpisode = { onEpisodeSelected(it) },
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onAddToQueue = { /* No se puede añadir desde la propia cola */ },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { ep, onMsgCallback ->
                                downloadedViewModel.downloadEpisodio(ep) { message ->
                                    onMsgCallback(message)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            },
                            onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                            isDownloaded = downloadedEpisodios.any { it.id == episodio.id },
                            isInQueue = true, // Todos en esta lista están en la cola
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}