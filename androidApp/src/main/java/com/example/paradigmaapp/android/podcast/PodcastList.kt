package com.example.paradigmaapp.android.podcast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Composable [PodcastList] que muestra una lista optimizada de [Podcast] utilizando [LazyColumn].
 * [LazyColumn] proporciona eficiencia al renderizar solo los elementos que son actualmente visibles en la pantalla.
 *
 * @param podcasts La lista de objetos [Podcast] a mostrar.
 * @param onPodcastSelected Lambda que se ejecuta cuando el usuario hace clic en un elemento de la lista,
 * recibiendo el [Podcast] seleccionado como parámetro.
 * @param modifier Modificador opcional para aplicar al LazyColumn.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun PodcastList(
    podcasts: List<Podcast>,
    onPodcastSelected: (Podcast) -> Unit,
    onAddToQueue: (Podcast) -> Unit,
    onRemoveFromQueue: (Podcast) -> Unit,
    onDownloadPodcast: (Podcast, (String) -> Unit) -> Unit,
    onDeleteDownload: (Podcast) -> Unit,
    downloadedPodcastIdentifiers: List<String>,
    queuePodcastUrls: List<String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(podcasts) { podcast ->
            PodcastListItem(
                podcast = podcast,
                onPodcastSelected = onPodcastSelected,
                onAddToQueue = onAddToQueue,
                onRemoveFromQueue = onRemoveFromQueue,
                onDownloadPodcast = onDownloadPodcast,
                onDeleteDownload = onDeleteDownload,
                isDownloaded = downloadedPodcastIdentifiers.contains(podcast.identifier),
                isInQueue = queuePodcastUrls.contains(podcast.url),
                downloadedPodcastIdentifiers = downloadedPodcastIdentifiers,
                queuePodcastUrls = queuePodcastUrls
            )
        }
    }
}