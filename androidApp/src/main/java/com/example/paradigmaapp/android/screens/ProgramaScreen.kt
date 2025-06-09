package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.ProgramaInfoHeader
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Muestra la pantalla de detalles de un programa, incluyendo su información
 * en una cabecera y una lista paginada de sus episodios.
 *
 * @param programaViewModel ViewModel específico para los datos de este programa.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción.
 * @param downloadedViewModel ViewModel para gestionar las descargas.
 * @param onEpisodeLongClicked Lambda para manejar el clic largo en un episodio (ej. navegar a detalles).
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramaScreen(
    programaViewModel: ProgramaViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa los detalles del programa para la cabecera.
    val programa by programaViewModel.programa.collectAsState()
    // Conecta el Flow de PagingData a la UI de Compose.
    val lazyPagingItems = programaViewModel.episodiosPaginados.collectAsLazyPagingItems()

    // Estados para la Snackbar, descargas y cola.
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    // Recoge el ID del episodio que se está preparando para la reproducción.
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Cabecera con la información del programa.
                item {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    ProgramaInfoHeader(programa = programa)

                    if (programa != null) {
                        Text(
                            text = "Episodios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }

                // Lista de episodios paginados.
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id }
                ) { index ->
                    val episodio = lazyPagingItems[index]
                    if (episodio != null) {
                        // Determina si este ítem debe mostrar el indicador de carga.
                        val isLoading = episodio.id == preparingEpisodeId

                        EpisodioListItem(
                            episodio = episodio,
                            isLoading = isLoading, // Se pasa el estado de carga
                            onPlayEpisode = { mainViewModel.selectEpisode(it) },
                            onEpisodeLongClick = onEpisodeLongClicked,
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { ep, onMsg ->
                                downloadedViewModel.downloadEpisodio(ep) { message ->
                                    onMsg(message)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            },
                            onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                            isDownloaded = downloadedEpisodios.any { it.id == episodio.id },
                            isInQueue = queueEpisodeIds.contains(episodio.id),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Indicadores de estado para la carga de más episodios.
                item {
                    when (val state = lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        is LoadState.Error -> {
                            ErrorView(message = "Error al cargar más: ${state.error.localizedMessage}", onRetry = { lazyPagingItems.retry() })
                        }
                        else -> {}
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(160.dp))
                }
            }

            // Indicadores de estado para la carga inicial.
            when (val state = lazyPagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LoadState.Error -> {
                    ErrorView(
                        message = "Error al cargar episodios:\n${state.error.localizedMessage}",
                        onRetry = { lazyPagingItems.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.append.endOfPaginationReached) {
                        Box(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                            Text("No hay episodios disponibles para este programa.", textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Botón para volver atrás.
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}