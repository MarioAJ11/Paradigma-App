package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription // Importación clave
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.*
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    episodeDetailViewModel: EpisodeDetailViewModel,
    mainViewModel: MainViewModel,
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

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    fun formatIsoDate(isoDate: String?): String {
        if (isoDate == null) return "Fecha desconocida"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("GMT")
            val date = inputFormat.parse(isoDate)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yy", Locale("es", "ES"))
            outputFormat.timeZone = TimeZone.getDefault()
            date?.let { outputFormat.format(it) } ?: "Fecha inválida"
        } catch (e: Exception) {
            "Fecha no procesable"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    onRetry = { episodeDetailViewModel.loadEpisodeDetails() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            episodio != null -> {
                val ep = episodio!!
                val isDownloaded = downloadedEpisodeIds.contains(ep.id)
                val isInQueue = queueEpisodeIds.contains(ep.id)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                    AsyncImage(
                        model = ep.imageUrl,
                        contentDescription = "Portada de ${ep.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.logo_foreground),
                        placeholder = painterResource(R.mipmap.logo_foreground)
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Text(
                            text = ep.title.unescapeHtmlEntities(),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { mainViewModel.selectEpisode(ep, playWhenReady = true) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
                            }

                            if (isInQueue) {
                                OutlinedIconButton(
                                    onClick = { queueViewModel.removeEpisodeFromQueue(ep) },
                                    modifier = Modifier.size(48.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Quitar de cola")
                                }
                            } else {
                                OutlinedIconButton(
                                    onClick = { queueViewModel.addEpisodeToQueue(ep) },
                                    modifier = Modifier.size(48.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Añadir a cola")
                                }
                            }

                            if (isDownloaded) {
                                OutlinedIconButton(
                                    onClick = { downloadedViewModel.deleteDownloadedEpisodio(ep) },
                                    modifier = Modifier.size(48.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Borrar Descarga")
                                }
                            } else {
                                OutlinedIconButton(
                                    onClick = {
                                        downloadedViewModel.downloadEpisodio(ep) { message ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = "Descargar")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- MODIFICACIÓN CLAVE AQUÍ ---
                        val episodioApiContent = ep.content // El HTML completo del contenido del episodio
                        val episodioApiExcerpt = ep.excerpt // El HTML del extracto

                        // Priorizar el contenido principal para extraer el primer párrafo.
                        // Usar el extracto solo como fallback si el contenido principal está vacío.
                        val displayDescription: String? = if (!episodioApiContent.isNullOrBlank()) {
                            episodioApiContent.extractMeaningfulDescription()
                        } else if (!episodioApiExcerpt.isNullOrBlank()) {
                            // Solo usar el extracto si el contenido principal no está disponible
                            episodioApiExcerpt.extractMeaningfulDescription()
                        } else {
                            null
                        }

                        if (!displayDescription.isNullOrBlank()) {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // --- FIN DE LA MODIFICACIÓN CLAVE ---
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            else -> {
                ErrorView(
                    message = "No se pudo cargar el episodio.",
                    errorType = ErrorType.NO_RESULTS,
                    onRetry = { episodeDetailViewModel.loadEpisodeDetails() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 8.dp,
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 8.dp
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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