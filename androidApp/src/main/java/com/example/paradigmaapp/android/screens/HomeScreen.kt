package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.ui.SearchBar // Si quieres un SearchBar aquí
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio

/**
 * Pantalla principal que muestra la lista de episodios.
 * Puede incluir una barra de búsqueda para filtrar la lista actual o navegar a SearchScreen.
 *
 * @param mainViewModel ViewModel principal para el estado global y episodios.
 * @param queueViewModel ViewModel para acciones de cola.
 * @param downloadedViewModel ViewModel para acciones de descarga.
 * @param onEpisodeSelected Lambda para manejar la selección de un episodio.
 * @param onNavigateToSearch Lambda para navegar a la pantalla de búsqueda dedicada.
 * @author Mario Alguacil Juárez
 */
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val initialEpisodios by mainViewModel.initialEpisodios.collectAsState()
    val isLoading by mainViewModel.isLoadingInitial.collectAsState()
    val context = LocalContext.current

    // Estados para el SearchBar local si decides tener uno aquí además de la pantalla de búsqueda
    var localSearchText by remember { mutableStateOf("") }
    val filteredEpisodios = remember(initialEpisodios, localSearchText) {
        if (localSearchText.isBlank()) {
            initialEpisodios
        } else {
            initialEpisodios.filter {
                it.title.contains(localSearchText, ignoreCase = true) ||
                        it.excerpt?.contains(localSearchText, ignoreCase = true) == true
            }
        }
    }

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()


    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredEpisodios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (localSearchText.isBlank()) "No hay episodios disponibles."
                    else "No se encontraron episodios para \"$localSearchText\".",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredEpisodios, key = { it.id }) { episodio ->
                    EpisodioListItem(
                        episodio = episodio,
                        onEpisodeSelected = onEpisodeSelected,
                        onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                        onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                        onDownloadEpisode = { ep, onMsg ->
                            downloadedViewModel.downloadEpisodio(ep, onMsg)
                        },
                        onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                        isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                        isInQueue = queueEpisodeIds.contains(episodio.id)
                    )
                }
            }
        }
    }
}