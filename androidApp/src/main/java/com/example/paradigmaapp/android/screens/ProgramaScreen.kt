package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.ui.ErrorType
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
 * Pantalla que muestra los detalles de un [Programa] específico, incluyendo su descripción
 * y una lista de sus [Episodio]s asociados.
 *
 * @param programaViewModel ViewModel que gestiona la lógica y el estado del programa.
 * @param mainViewModel ViewModel para interactuar con la cola de reproducción.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción.
 * @param downloadedViewModel ViewModel para interactuar con el estado de las descargas.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un episodio (ej. ver detalles).
 * @param onBackClick Lambda para manejar la acción de retroceso y cerrar esta pantalla.
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
    val programa by programaViewModel.programa.collectAsState()
    val episodios by programaViewModel.episodios.collectAsState()
    val isLoadingPrograma by programaViewModel.isLoadingPrograma.collectAsState()
    val isLoadingEpisodios by programaViewModel.isLoadingEpisodios.collectAsState()
    val error by programaViewModel.error.collectAsState()

    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (error != null && programa == null && !isLoadingPrograma) {
                val errorType = if (error!!.contains("internet", ignoreCase = true)) ErrorType.NO_INTERNET else ErrorType.GENERAL_SERVER_ERROR
                ErrorView(
                    message = error!!,
                    errorType = errorType,
                    onRetry = { programaViewModel.loadProgramaConEpisodios() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    }
                    item {
                        ProgramaInfoHeader(programa = programa, isLoading = isLoadingPrograma)
                    }
                    if (programa != null) {
                        item {
                            Text(
                                text = "Episodios",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                            )
                        }

                        // Lógica para mostrar la lista de episodios, la carga o el error de episodios
                        when {
                            isLoadingEpisodios && episodios.isEmpty() -> {
                                item { Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), Alignment.Center) { CircularProgressIndicator() } }
                            }
                            error != null && episodios.isEmpty() && !isLoadingEpisodios -> {
                                item { ErrorView(message = "Error al cargar episodios: $error", onRetry = { programaViewModel.loadProgramaConEpisodios() }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) }
                            }
                            episodios.isEmpty() && !isLoadingEpisodios && error == null -> {
                                item { Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) { Text("No hay episodios disponibles para este programa.", textAlign = TextAlign.Center) } }
                            }
                            episodios.isNotEmpty() -> {
                                items(episodios, key = { episodio -> episodio.id }) { episodio ->
                                    EpisodioListItem(
                                        episodio = episodio,
                                        onPlayEpisode = { mainViewModel.selectEpisode(it) },
                                        onEpisodeLongClick = { onEpisodeLongClicked(it) },
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
                        }
                    }
                }
            }

            // Botón de retroceso
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Composable auxiliar para mostrar la cabecera con la información del programa.
 *
 * @param programa El [Programa] para mostrar.
 * @param isLoading Indica si la carga del programa está en curso.
 */
@Composable
private fun ProgramaInfoHeader(programa: Programa?, isLoading: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading && programa == null) {
            Box(Modifier.height(280.dp), Alignment.Center) { CircularProgressIndicator() }
        } else {
            programa?.let { prog ->
                AsyncImage(
                    model = prog.imageUrl,
                    contentDescription = "Portada de ${prog.name.unescapeHtmlEntities()}",
                    modifier = Modifier.size(180.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.logo_foreground),
                    placeholder = painterResource(R.mipmap.logo_foreground)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = prog.name.unescapeHtmlEntities(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                prog.description?.takeIf { it.isNotBlank() }?.let { desc ->
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
}