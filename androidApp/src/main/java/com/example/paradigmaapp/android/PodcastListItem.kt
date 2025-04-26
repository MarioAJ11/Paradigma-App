package com.example.paradigmaapp.android

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme

/**
 * [PodcastListItem] composable renders a single podcast item in a list.
 * It displays the podcast's cover image, title, and duration.
 * A [CircularProgressIndicator] is shown as long as the title is empty,
 * indicating that the basic podcast information is still being loaded.
 *
 * @param podcast The [Podcast] data object to be displayed.
 * @param onPodcastSelected Callback triggered when this item is clicked,
 * passing the selected [Podcast] to the caller (e.g., [PodcastList]).
 */
@Composable
fun PodcastListItem(podcast: Podcast, onPodcastSelected: (Podcast) -> Unit) {
    val isDetailsLoaded by remember {
        derivedStateOf { podcast.title.isNotEmpty() }
    }

    Log.d("PodcastListItem", "🎧 Mostrando item - Título: ${podcast.title}, Duración: ${podcast.duration}, Identifier: ${podcast.identifier}")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onPodcastSelected(podcast) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = "${podcast.title} cover image",
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!isDetailsLoaded) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        text = podcast.duration,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}