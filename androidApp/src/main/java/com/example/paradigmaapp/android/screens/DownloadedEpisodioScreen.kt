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
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel // ViewModel para esta pantalla
import com.example.paradigmaapp.android.viewmodel.QueueViewModel // ViewModel para la cola
import com.example.paradigmaapp.model.Episodio // Modelo de datos del episodio
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra los episodios que han sido descargados por el usuario.
 *
 * @param downloadedEpisodioViewModel ViewModel para los episodios descargados.
 * @param queueViewModel ViewModel para gestionar acciones de la cola.
 * @param onEpisodeSelected Lambda que se invoca cuando un episodio es seleccionado para reproducción.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para componentes experimentales de Material 3 como TopAppBar
@Composable
fun DownloadedEpisodioScreen(
    downloadedEpisodioViewModel: DownloadedEpisodioViewModel,
    queueViewModel: QueueViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa la lista de episodios descargados desde el ViewModel.
    val downloadedEpisodios by downloadedEpisodioViewModel.downloadedEpisodios.collectAsState()
    // Observa los IDs de los episodios en cola para el estado en EpisodioListItem.
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    // Para el Snackbar, para mostrar mensajes
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Host para los Snackbars
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Descargas", // Título de la pantalla
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface // Color del texto del título
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { // Botón para volver atrás
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, // Icono de flecha
                            contentDescription = "Volver", // Descripción para accesibilidad
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // Color del icono
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Colores de la TopAppBar
                    containerColor = MaterialTheme.colorScheme.surface // Color de fondo de la TopAppBar
                )
            )
        }
    ) { innerPadding -> // Padding proporcionado por el Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding del Scaffold
                .background(MaterialTheme.colorScheme.background) // Color de fondo de la pantalla
        ) {
            // Muestra un mensaje si no hay episodios descargados
            if (downloadedEpisodios.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp), // Padding alrededor del texto
                    contentAlignment = Alignment.Center // Centra el contenido
                ) {
                    Text(
                        text = "No tienes episodios descargados.",
                        style = MaterialTheme.typography.bodyLarge, // Estilo del texto
                        color = MaterialTheme.colorScheme.onBackground, // Color del texto
                        textAlign = TextAlign.Center // Alineación del texto
                    )
                }
            } else {
                // Lista de episodios descargados
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp) // Padding vertical para la lista
                ) {
                    items(downloadedEpisodios, key = { it.id }) { episodio ->
                        // Cada item de la lista es un EpisodioListItem.
                        EpisodioListItem(
                            episodio = episodio,
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onPlayEpisode = { onEpisodeSelected(it) }, // Acción de reproducir al hacer clic
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) }, // Añadir a la cola
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) }, // Quitar de la cola
                            onDownloadEpisode = { _, onMsgCallback -> // Acción de descargar (ya está descargado)
                                val message = "Este episodio ya está descargado."
                                onMsgCallback(message) // Llama al callback con el mensaje
                                coroutineScope.launch { // Muestra el Snackbar
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                            onDeleteDownload = { downloadedEpisodioViewModel.deleteDownloadedEpisodio(it) }, // Eliminar descarga
                            isDownloaded = true, // Todos los items aquí están descargados
                            isInQueue = queueEpisodeIds.contains(episodio.id), // Verifica si está en la cola
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp) // Padding para cada item
                        )
                    }
                }
            }
        }
    }
}