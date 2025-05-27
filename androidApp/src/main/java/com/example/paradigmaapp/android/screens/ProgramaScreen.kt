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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Tus recursos
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.utils.extractTextFromHtml
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.model.Programa // Modelo Programa
import timber.log.Timber

/**
 * Pantalla que muestra los detalles de un programa y la lista de sus episodios.
 * La cabecera incluye una flecha de retroceso flotante, imagen centrada, título y descripción.
 *
 * @param programaViewModel ViewModel específico para esta pantalla.
 * @param mainViewModel ViewModel principal para acciones como seleccionar un episodio para reproducción.
 * @param queueViewModel ViewModel para acciones de cola.
 * @param downloadedViewModel ViewModel para acciones de descarga.
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
    // programaNombreFallback ya no es tan necesario si el título principal está en el cuerpo
    onBackClick: () -> Unit
) {
    val programa by programaViewModel.programa.collectAsState()
    val episodios by programaViewModel.episodios.collectAsState()
    val isLoadingPrograma by programaViewModel.isLoadingPrograma.collectAsState()
    val isLoadingEpisodios by programaViewModel.isLoadingEpisodios.collectAsState()
    val error by programaViewModel.error.collectAsState()

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    // Usaremos un Box para poder superponer la flecha de "Volver"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Contenido principal que se puede desplazar
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, // Padding superior para no solapar con la barra de estado y dar espacio
                bottom = 16.dp
            )
        ) {
            // Item para la Cabecera del Programa (Imagen, Título, Descripción)
            item {
                // Esta Column contendrá la imagen, título y descripción.
                // El IconButton de "volver" estará fuera de esta Column para posicionamiento absoluto.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), // Padding horizontal para el contenido de la cabecera
                    horizontalAlignment = Alignment.CenterHorizontally // Centra la imagen
                ) {
                    programa?.let { prog -> // Solo muestra si el programa no es nulo
                        // Imagen del Programa (Centrada)
                        AsyncImage(
                            model = prog.imageUrl,
                            contentDescription = "Portada de ${prog.name.unescapeHtmlEntities()}",
                            modifier = Modifier
                                .size(180.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    clip = false
                                )
                                .clip(RoundedCornerShape(12.dp)),

                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground),
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Título del Programa (Alineado a la izquierda por defecto en Text)
                        Text(
                            text = prog.name.unescapeHtmlEntities(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Descripción del Programa
                        prog.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc.extractTextFromHtml().unescapeHtmlEntities(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } ?: run {
                        // Muestra un indicador de carga o mensaje si 'programa' es nulo pero no hay error
                        if (isLoadingPrograma || (error == null && episodios.isEmpty())) {
                            Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center){ // Altura placeholder para la cabecera
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // Item para la etiqueta "Episodios" (Solo si hay programa)
            if (programa != null) {
                item {
                    Text(
                        text = "Episodios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp) // Añadir top padding
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Lógica para mostrar episodios (carga, vacío, lista)
            when {
                isLoadingEpisodios && episodios.isEmpty() && programa != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                episodios.isEmpty() && !isLoadingEpisodios && programa != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay episodios disponibles para este programa.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                episodios.isNotEmpty() && programa != null -> {
                    items(episodios, key = { episodio -> episodio.id }) { episodio ->
                        EpisodioListItem(
                            episodio = episodio,
                            onPlayEpisode = { mainViewModel.selectEpisode(it) },
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { ep, onMsg ->
                                downloadedViewModel.downloadEpisodio(ep, onMsg)
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

        // Flecha de "Volver" superpuesta en la parte superior izquierda
        // Se alinea con el padding de la status bar más un poco de margen.
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart) // Alinea el botón en la esquina superior izquierda del Box
                .padding( // Padding para separarlo de los bordes y de la barra de estado
                    start = 8.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f), CircleShape) // Fondo semi-transparente opcional
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface // Color del icono
            )
        }

        // Mensaje de error global (si 'programa' es null y hay un error)
        if (programa == null && error != null && !isLoadingPrograma) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}