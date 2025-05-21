// android/screens/OnGoingPodcastScreen.kt
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
import com.example.paradigmaapp.android.viewmodel.OnGoingPodcastViewModel

@Composable
fun OnGoingPodcastScreen(
    onGoingPodcastViewModel: OnGoingPodcastViewModel,
    onPodcastSelected: (Podcast) -> Unit,
    onBackClick: () -> Unit
) {
    val onGoingPodcasts by onGoingPodcastViewModel.onGoingPodcasts.collectAsState()

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
                text = "Seguir Viendo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onGoingPodcasts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay podcasts en progreso.",
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
                items(onGoingPodcasts) { podcast ->
                    // --- CORRECCIÓN AQUÍ ---
                    PodcastListItem(
                        podcast = podcast,
                        onPodcastSelected = onPodcastSelected,
                        onAddToQueue = { /* No se gestiona la cola desde aquí */ },
                        onRemoveFromQueue = { /* No se gestiona la cola desde aquí */ },
                        onDownloadPodcast = { _, _ -> /* No se gestionan descargas desde aquí */ },
                        onDeleteDownload = { /* No se gestionan descargas desde aquí */ },
                        isDownloaded = false, // En esta pantalla, no es relevante si está descargado
                        isInQueue = false,     // En esta pantalla, no es relevante si está en cola
                        downloadedPodcastIdentifiers = emptyList(), // No relevantes para esta vista
                        queuePodcastUrls = emptyList() // No relevantes para esta vista
                    )
                }
            }
        }
    }
}