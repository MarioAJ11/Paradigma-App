package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra la lista de episodios descargados por el usuario.
 * Permite al usuario ver sus descargas, reproducirlas, gestionarlas en la cola
 * o eliminar las descargas.
 *
 * @param downloadedEpisodioViewModel ViewModel que gestiona la lógica y el estado de los episodios descargados.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción (necesario para las acciones del [EpisodioListItem]).
 * @param onEpisodeSelected Lambda que se invoca cuando un episodio es seleccionado para su reproducción.
 * @param onEpisodeLongClicked Lambda que se invoca para acciones contextuales sobre un episodio (ej. ver detalles).
 * @param onBackClick Lambda para manejar la acción de retroceso y cerrar esta pantalla.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedEpisodioScreen(
    downloadedEpisodioViewModel: DownloadedEpisodioViewModel,
    queueViewModel: QueueViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    val downloadedEpisodios by downloadedEpisodioViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState() // Necesario para el estado 'isInQueue' de EpisodioListItem

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Descargas",
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
                    containerColor = MaterialTheme.colorScheme.surface // Color de fondo de la TopAppBar
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding del Scaffold
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (downloadedEpisodios.isEmpty()) {
                // Muestra un mensaje si no hay episodios descargados.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes episodios descargados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // Un color más sutil para el texto vacío
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de episodios descargados.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp) // Padding para la lista
                ) {
                    items(downloadedEpisodios, key = { episodio -> episodio.id }) { episodio ->
                        EpisodioListItem(
                            episodio = episodio,
                            onPlayEpisode = { onEpisodeSelected(it) },
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { _, onMsgCallback ->
                                // El episodio ya está descargado, informar al usuario.
                                val message = "'${episodio.title}' ya está descargado."
                                onMsgCallback(message) // Permite al llamador (si lo hubiera) saber del mensaje.
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                            onDeleteDownload = { downloadedEpisodioViewModel.deleteDownloadedEpisodio(it) },
                            isDownloaded = true, // Todos los episodios en esta pantalla están descargados.
                            isInQueue = queueEpisodeIds.contains(episodio.id), // Verifica si el episodio está en la cola.
                            modifier = Modifier.padding(vertical = 4.dp) // Padding vertical para cada ítem
                        )
                    }
                }
            }
        }
    }
}