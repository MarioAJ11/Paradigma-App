package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.EpisodeDetailViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pantalla que muestra los detalles completos de un [Episodio] específico.
 * Proporciona información como título, imagen, descripción, fecha, duración,
 * y programas asociados. Ofrece acciones como reproducir, añadir/quitar de cola
 * y descargar/eliminar descarga.
 *
 * @param episodeDetailViewModel ViewModel que gestiona la carga y el estado del episodio actual.
 * @param mainViewModel ViewModel principal para acciones de reproducción.
 * @param queueViewModel ViewModel para gestionar la cola de reproducción.
 * @param downloadedViewModel ViewModel para gestionar las descargas.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    episodeDetailViewModel: EpisodeDetailViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onBackClick: () -> Unit
) {
    val episodioState by episodeDetailViewModel.episodio.collectAsState()
    val programasAsociados by episodeDetailViewModel.programasAsociados.collectAsState()
    val isLoading by episodeDetailViewModel.isLoading.collectAsState()
    val error by episodeDetailViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 72.dp)) },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && episodioState == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null && episodioState == null -> {
                    val errorType = if (error!!.contains("internet", ignoreCase = true)) {
                        ErrorType.NO_INTERNET
                    } else if (error!!.contains("No se pudo encontrar", ignoreCase = true)) {
                        ErrorType.NO_RESULTS
                    } else {
                        ErrorType.GENERAL_SERVER_ERROR
                    }
                    ErrorView(
                        message = error!!,
                        errorType = errorType,
                        onRetry = { episodeDetailViewModel.loadEpisodeDetails() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                episodioState != null -> {
                    val episodio = episodioState!!
                    val isDownloaded = downloadedEpisodios.any { it.id == episodio.id }
                    val isInQueue = queueEpisodeIds.contains(episodio.id)

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                        AsyncImage(
                            model = episodio.imageUrl,
                            contentDescription = "Portada de ${episodio.title.unescapeHtmlEntities()}", // *** CORREGIDO ***
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground),
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = episodio.title.unescapeHtmlEntities(), // *** CORREGIDO ***
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaDataItem(Icons.Default.DateRange, formatarFechaIso(episodio.date))
                                if (episodio.duration.isNotBlank() && episodio.duration != "--:--") {
                                    MetaDataItem(Icons.Default.Timer, episodio.duration)
                                }
                            }
                            if (programasAsociados.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Programa: ${programasAsociados.joinToString { it.name.unescapeHtmlEntities() }}", // *** CORREGIDO ***
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            // Botones de acción
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(onClick = { mainViewModel.selectEpisode(episodio, true) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.PlayArrow, "Reproducir episodio") }
                                val queueIcon = if (isInQueue) Icons.Filled.RemoveCircleOutline else Icons.Filled.PlaylistAdd
                                val queueAction = if (isInQueue) "Quitar de cola" else "Añadir a cola"
                                OutlinedIconButton(onClick = { if (isInQueue) queueViewModel.removeEpisodeFromQueue(episodio) else queueViewModel.addEpisodeToQueue(episodio) }, modifier = Modifier.size(48.dp), border = ButtonDefaults.outlinedButtonBorder) { Icon(queueIcon, queueAction) }
                                val downloadIcon = if (isDownloaded) Icons.Filled.DeleteOutline else Icons.Filled.Download
                                val downloadAction = if (isDownloaded) "Borrar Descarga" else "Descargar episodio"
                                OutlinedIconButton(onClick = { if (isDownloaded) downloadedViewModel.deleteDownloadedEpisodio(episodio) else downloadedViewModel.downloadEpisodio(episodio) { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) } } }, modifier = Modifier.size(48.dp), border = ButtonDefaults.outlinedButtonBorder) { Icon(downloadIcon, downloadAction) }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val displayDescription = episodio.content?.extractMeaningfulDescription()?.takeIf { it.isNotBlank() } // *** CORREGIDO ***
                                ?: episodio.excerpt?.extractMeaningfulDescription()?.takeIf { it.isNotBlank() } // *** CORREGIDO ***

                            if (!displayDescription.isNullOrBlank()) {
                                Text(text = "Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = displayDescription, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                else -> {
                    ErrorView(message = "No se pudo cargar la información del episodio.", errorType = ErrorType.NO_RESULTS, onRetry = { episodeDetailViewModel.loadEpisodeDetails() }, modifier = Modifier.align(Alignment.Center))
                }
            }
            // Botón de retroceso
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

/** Formatea una fecha ISO 8601 (GMT) a un formato legible en español. */
private fun formatarFechaIso(isoDate: String?): String {
    if (isoDate == null) return "Fecha desconocida"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
        inputFormat.timeZone = TimeZone.getTimeZone("GMT")
        val date = inputFormat.parse(isoDate)
        val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        outputFormat.timeZone = TimeZone.getDefault()
        date?.let { outputFormat.format(it) } ?: "Fecha inválida"
    } catch (e: Exception) { "Fecha no procesable" }
}

/** Un Composable auxiliar para mostrar un metadato con un icono y texto. */
@Composable
private fun MetaDataItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}