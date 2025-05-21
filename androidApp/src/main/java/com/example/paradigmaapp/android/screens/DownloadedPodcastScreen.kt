// android/screens/DownloadedPodcastScreen.kt
package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.podcast.PodcastListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedPodcastViewModel

@Composable
fun DownloadedPodcastScreen(
    downloadedPodcastViewModel: DownloadedPodcastViewModel,
    onPodcastSelected: (Podcast) -> Unit,
    onBackClick: () -> Unit
) {
    val downloadedPodcasts by downloadedPodcastViewModel.downloadedPodcasts.collectAsState()
    val downloadedPodcastIdentifiers by downloadedPodcastViewModel.downloadedPodcastIdentifiers.collectAsState() // Para el estado de los ítems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra superior con título y botón de retroceso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mis Descargas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (downloadedPodcasts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay podcasts descargados.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(downloadedPodcasts) { podcast ->
                    // --- CORRECCIÓN AQUÍ ---
                    PodcastListItem(
                        podcast = podcast,
                        onPodcastSelected = onPodcastSelected,
                        onAddToQueue = { /* No aplicable directamente aquí */ },
                        onRemoveFromQueue = { /* No aplicable directamente aquí */ },
                        onDownloadPodcast = { _, _ -> /* No aplicable directamente aquí */ }, // La descarga se maneja en la lista principal
                        onDeleteDownload = { downloadedPodcastViewModel.deleteDownloadedPodcast(it) }, // Permitir eliminar descarga
                        isDownloaded = downloadedPodcastIdentifiers.contains(podcast.identifier), // Indicar si está descargado
                        isInQueue = false, // En esta pantalla, no es relevante si está en cola
                        downloadedPodcastIdentifiers = downloadedPodcastIdentifiers, // Pasar el estado real de descarga
                        queuePodcastUrls = emptyList() // No relevante para esta vista
                    )
                }
            }
        }
    }
}