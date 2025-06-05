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
import androidx.compose.foundation.lazy.items
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
 * @param programaViewModel ViewModel que gestiona la carga y el estado del programa y sus episodios.
 * @param mainViewModel ViewModel principal para acciones de reproducción.
 * @param queueViewModel ViewModel para gestionar la cola de reproducción.
 * @param downloadedViewModel ViewModel para gestionar las descargas.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un episodio (ej. ver detalles).
 * @param onBackClick Lambda para manejar la acción de retroceso.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold y otros componentes M3
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

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Usar Scaffold permite una estructura más estándar y facilita añadir elementos como SnackBar si es necesario.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding) // Aplicar padding del Scaffold
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Estado de error principal (si no se pudo cargar la información del programa)
            if (error != null && programa == null && !isLoadingPrograma) {
                val errorType = if (error!!.contains("internet", ignoreCase = true) || error!!.contains("conectar", ignoreCase = true)) {
                    ErrorType.NO_INTERNET
                } else {
                    ErrorType.GENERAL_SERVER_ERROR
                }
                ErrorView(
                    message = error!!,
                    errorType = errorType,
                    onRetry = { programaViewModel.loadProgramaConEpisodios() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Contenido principal de la pantalla (información del programa y lista de episodios)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp) // Padding inferior para el último ítem
                ) {
                    // Espacio para la barra de estado
                    item {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    }

                    // Sección de información del programa
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp), // Padding superior para la info del programa
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLoadingPrograma && programa == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp), // Altura aproximada para la sección de info del programa
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                programa?.let { prog ->
                                    AsyncImage(
                                        model = prog.imageUrl,
                                        contentDescription = "Portada de ${prog.name.unescapeHtmlEntities()}",
                                        modifier = Modifier
                                            .size(180.dp)
                                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), clip = false)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(R.mipmap.logo_foreground),
                                        placeholder = painterResource(R.mipmap.logo_foreground)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = prog.name.unescapeHtmlEntities(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    prog.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                        Text(
                                            text = desc.extractMeaningfulDescription(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center // Centrar descripción si es corta
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    // Sección de la lista de episodios
                    if (programa != null) { // Mostrar sección de episodios solo si hay información del programa
                        item {
                            Text(
                                text = "Episodios",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                            )
                        }

                        when {
                            isLoadingEpisodios && episodios.isEmpty() -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 50.dp),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                }
                            }
                            error != null && episodios.isEmpty() && !isLoadingEpisodios -> {
                                // Error específico al cargar episodios, pero la info del programa ya se mostró.
                                item {
                                    val errorTypeEpisodios = if (error!!.contains("internet", ignoreCase = true) || error!!.contains("conectar", ignoreCase = true)) {
                                        ErrorType.NO_INTERNET
                                    } else {
                                        ErrorType.GENERAL_SERVER_ERROR
                                    }
                                    ErrorView(
                                        message = "Error al cargar episodios: $error",
                                        errorType = errorTypeEpisodios,
                                        onRetry = { programaViewModel.loadProgramaConEpisodios() },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp) // Dar más espacio al error de episodios
                                    )
                                }
                            }
                            episodios.isEmpty() && !isLoadingEpisodios && error == null -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay episodios disponibles para este programa.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
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
                                            downloadedViewModel.downloadEpisodio(ep, onMsg)
                                            // El mensaje se maneja en el callback de downloadEpisodio
                                            // para mostrar Snackbar si es necesario.
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Iniciando descarga de '${ep.title}'...")
                                            }
                                        },
                                        onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                                        isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                                        isInQueue = queueEpisodeIds.contains(episodio.id),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Botón de retroceso superpuesto
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = 8.dp,
                        top = WindowInsets.statusBars //
                            .asPaddingValues()
                            .calculateTopPadding() + 8.dp
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape) // Fondo semi-transparente
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface // Color del icono para contraste
                )
            }
        }
    }
}