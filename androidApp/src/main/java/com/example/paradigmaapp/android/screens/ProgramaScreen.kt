package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import kotlinx.coroutines.launch

/**
 * Composable que representa la pantalla de detalles de un Programa.
 * Muestra una cabecera con la información del programa y una lista paginada de sus episodios.
 *
 * @param programaViewModel El ViewModel que gestiona los datos del programa.
 * @param mainViewModel El ViewModel principal de la aplicación.
 * @param queueViewModel El ViewModel para la gestión de la cola de reproducción.
 * @param downloadedViewModel El ViewModel para la gestión de episodios descargados.
 * @param onEpisodeLongClicked Callback para manejar el clic largo en un episodio.
 * @param onBackClick Callback para manejar el clic en el botón de retroceso.
 *
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
    // --- OBSERVACIÓN DEL ESTADO ---
    // Observa los detalles del programa para la cabecera.
    val programa by programaViewModel.programa.collectAsState()
    // Conecta el Flow de PagingData a la UI de Compose.
    val lazyPagingItems = programaViewModel.episodiosPaginados.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

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
                // --- CABECERA ---
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

                // --- LISTA DE EPISODIOS PAGINADOS ---
                items(
                    count = lazyPagingItems.itemCount,
                    // La key ayuda a Compose a optimizar la recomposición de la lista.
                    key = lazyPagingItems.itemKey { it.id }
                ) { index ->
                    val episodio = lazyPagingItems[index]
                    if (episodio != null) {
                        EpisodioListItem(
                            episodio = episodio,
                            onPlayEpisode = { mainViewModel.selectEpisode(episodio) },
                            onEpisodeLongClick = { onEpisodeLongClicked(episodio) },
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(episodio) },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(episodio) },
                            onDownloadEpisode = { ep, onMsg ->
                                downloadedViewModel.downloadEpisodio(ep) { message ->
                                    onMsg(message)
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            },
                            onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(episodio) },
                            isDownloaded = downloadedEpisodios.any { it.id == episodio.id },
                            isInQueue = queueEpisodeIds.contains(episodio.id),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // --- ESTADO DE CARGA DE LA SIGUIENTE PÁGINA ---
                item {
                    // 'append' se refiere al estado de carga al final de la lista.
                    when (val state = lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            // Muestra un spinner al final mientras carga la siguiente página.
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        is LoadState.Error -> {
                            // Muestra un error si falla la carga de la siguiente página.
                            ErrorView(message = "Error al cargar más: ${state.error.localizedMessage}", onRetry = { lazyPagingItems.retry() })
                        }
                        else -> {} // No hacer nada si no está cargando ni en error.
                    }
                }
            }

            // --- ESTADO DE CARGA INICIAL DE LA PANTALLA ---
            // 'refresh' se refiere al estado de la carga inicial de toda la lista.
            when (val state = lazyPagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    // Muestra un spinner en el centro durante la primera carga.
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LoadState.Error -> {
                    // Muestra un error a pantalla completa si la carga inicial falla.
                    ErrorView(
                        message = "Error al cargar episodios:\n${state.error.localizedMessage}",
                        onRetry = { lazyPagingItems.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Cuando la carga termina, comprueba si la lista final está vacía.
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

/**
 * Composable auxiliar para la cabecera del programa.
 *
 * @param programa El objeto Programa que se mostrará en la cabecera.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
private fun ProgramaInfoHeader(programa: Programa?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (programa == null) {
            // Muestra un indicador de carga mientras los detalles del programa llegan.
            Box(Modifier.height(280.dp), Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Cuando los detalles del programa están disponibles, se pinta la cabecera.
            AsyncImage(
                model = programa.imageUrl ?: programa.imagenDelPrograma?.guid,
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}",
                modifier = Modifier.size(180.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground),
                placeholder = painterResource(R.mipmap.logo_foreground)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = programa.name.unescapeHtmlEntities(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            programa.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc.extractMeaningfulDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}