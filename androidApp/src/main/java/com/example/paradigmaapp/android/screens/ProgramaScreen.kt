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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.ui.ProgramaHeader
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities

/**
 * Pantalla que muestra los detalles de un programa y la lista de sus episodios.
 *
 * @param programaViewModel ViewModel específico para esta pantalla.
 * @param mainViewModel ViewModel principal para acciones como seleccionar un episodio para reproducción.
 * @param queueViewModel ViewModel para acciones de cola.
 * @param downloadedViewModel ViewModel para acciones de descarga.
 * @param programaNombreFallback Nombre del programa (obtenido de args de nav) como fallback.
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
    programaNombreFallback: String,
    onBackClick: () -> Unit
) {
    val programa by programaViewModel.programa.collectAsState()
    val episodios by programaViewModel.episodios.collectAsState()
    val isLoadingPrograma by programaViewModel.isLoadingPrograma.collectAsState()
    val isLoadingEpisodios by programaViewModel.isLoadingEpisodios.collectAsState()
    val error by programaViewModel.error.collectAsState()

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    // El título de la pantalla se obtiene del programa cargado o del fallback.
    val tituloPantalla = programa?.name?.let { nombrePrograma ->
        // CORRECCIÓN AQUÍ: Llamar como función de extensión
        nombrePrograma.unescapeHtmlEntities()
    } ?: programaNombreFallback


    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = tituloPantalla,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ),
            modifier = Modifier.statusBarsPadding()
        )

        when {
            isLoadingPrograma || (programa == null && error == null && isLoadingEpisodios) -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            programa != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        // Usar el Composable ProgramaHeader importado
                        ProgramaHeader(programa = programa!!)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Episodios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isLoadingEpisodios && episodios.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (episodios.isEmpty() && !isLoadingEpisodios) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No hay episodios disponibles para este programa.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        items(episodios, key = { episodio -> episodio.id }) { episodio ->
                            EpisodioListItem(
                                episodio = episodio,
                                // CORREGIDO: onEpisodeClicked en lugar de onEpisodeSelected (si así lo tienes en EpisodioListItem)
                                onEpisodeClicked = { mainViewModel.selectEpisode(episodio) },
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(episodio) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(episodio) },
                                onDownloadEpisode = { ep, onMsg ->
                                    downloadedViewModel.downloadEpisodio(
                                        ep,
                                        onMsg
                                    )
                                },
                                onDeleteDownload = {
                                    downloadedViewModel.deleteDownloadedEpisodio(
                                        episodio
                                    )
                                },
                                isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                                isInQueue = queueEpisodeIds.contains(episodio.id),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onEpisodeSelected = TODO()
                            )
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No se pudo cargar la información del programa.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}