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
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription // Cambio aquí
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa

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

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingPrograma && programa == null) {
                            Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center){
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
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                prog.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                    Text(
                                        // Usar extractMeaningfulDescription para la descripción del programa
                                        text = desc.extractMeaningfulDescription(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }

                if (programa != null) {
                    item {
                        Text(
                            text = "Episodios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }
                }

                when {
                    isLoadingEpisodios && episodios.isEmpty() && programa != null -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                    error != null && episodios.isEmpty() && programa != null && !isLoadingEpisodios -> {
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
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    episodios.isEmpty() && !isLoadingEpisodios && programa != null && error == null -> {
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
                    episodios.isNotEmpty() -> {
                        items(episodios, key = { episodio -> episodio.id }) { episodio ->
                            EpisodioListItem(
                                episodio = episodio,
                                onPlayEpisode = { mainViewModel.selectEpisode(it) },
                                onEpisodeLongClick = { onEpisodeLongClicked(it) },
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                                onDownloadEpisode = { ep, onMsg -> downloadedViewModel.downloadEpisodio(ep, onMsg) },
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

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 8.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}