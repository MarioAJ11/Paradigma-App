package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.utils.extractTextFromHtml
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    episodeDetailViewModel: EpisodeDetailViewModel,
    mainViewModel: MainViewModel, // Para acciones de reproducción, etc.
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onBackClick: () -> Unit
) {
    val episodio by episodeDetailViewModel.episodio.collectAsState()
    val programasAsociados by episodeDetailViewModel.programasAsociados.collectAsState()
    val isLoading by episodeDetailViewModel.isLoading.collectAsState()
    val error by episodeDetailViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    fun formatIsoDate(isoDate: String?): String {
        if (isoDate == null) return "Fecha desconocida"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("GMT") // La fecha viene en GMT
            val date = inputFormat.parse(isoDate)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            outputFormat.timeZone = TimeZone.getDefault() // Mostrar en la zona horaria local
            date?.let { outputFormat.format(it) } ?: "Fecha inválida"
        } catch (e: Exception) {
            "Fecha no procesable"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        episodio?.title ?: "Detalle del Episodio",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) // Semi-transparente si se superpone
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && episodio == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    val errorType = if (error!!.contains("internet", ignoreCase = true) || error!!.contains("conectar", ignoreCase = true)) {
                        ErrorType.NO_INTERNET
                    } else {
                        ErrorType.GENERAL_SERVER_ERROR
                    }
                    ErrorView(
                        message = error!!,
                        errorType = errorType,
                        onRetry = { episodeDetailViewModel.loadEpisodeDetails() }
                    )
                }
                episodio != null -> {
                    val ep = episodio!! // Smart cast
                    val isDownloaded = downloadedEpisodeIds.contains(ep.id)
                    val isInQueue = queueEpisodeIds.contains(ep.id)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = ep.imageUrl,
                            contentDescription = "Portada de ${ep.title}",
                            modifier = Modifier
                                .size(200.dp)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground),
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = ep.title.unescapeHtmlEntities(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fila para Metadatos (Fecha, Duración)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetaDataItem(Icons.Default.DateRange, formatIsoDate(ep.date))
                            if (ep.duration.isNotBlank() && ep.duration != "--:--") {
                                MetaDataItem(Icons.Default.Timer, ep.duration)
                            }
                        }

                        if (programasAsociados.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Programa(s): ${programasAsociados.joinToString { it.name.unescapeHtmlEntities() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botones de Acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { mainViewModel.selectEpisode(ep, playWhenReady = true) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("Reproducir")
                            }

                            if (isInQueue) {
                                OutlinedButton(onClick = { queueViewModel.removeEpisodeFromQueue(ep) }) {
                                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Quitar de cola")
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("En Cola")
                                }
                            } else {
                                OutlinedButton(onClick = { queueViewModel.addEpisodeToQueue(ep) }) {
                                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Añadir a cola")
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("Añadir Cola")
                                }
                            }

                            if (isDownloaded) {
                                OutlinedButton(onClick = { downloadedViewModel.deleteDownloadedEpisodio(ep) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Borrar Descarga")
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("Descargado")
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    downloadedViewModel.downloadEpisodio(ep) { message ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Descargar")
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("Descargar")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ep.content?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Usar MarkdownText si el contenido es Markdown, o HTMLText si es HTML complejo.
                            // Por ahora, simple Text con limpieza básica.
                            Text(
                                text = it.extractTextFromHtml().unescapeHtmlEntities(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: ep.excerpt?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Resumen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it.extractTextFromHtml().unescapeHtmlEntities(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> { // Caso en que episodio es null y no hay error ni está cargando
                    ErrorView(
                        message = "No se pudo cargar el episodio.",
                        errorType = ErrorType.GENERAL_SERVER_ERROR, // O un tipo NO_RESULTS
                        onRetry = { episodeDetailViewModel.loadEpisodeDetails() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaDataItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}