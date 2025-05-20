package com.example.paradigmaapp.android.podcast

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator // Aunque ya no se usa aquí, la dejo por si acaso
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Aunque ya no se usa directamente, la dejo por si acaso
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip

/**
 * Composable que representa un elemento individual en la lista de podcasts.
 * Muestra la carátula, título y duración del podcast, y permite la selección al hacer clic.
 *
 * @param podcast El [Podcast] que se va a mostrar en la lista.
 * @param onPodcastSelected Lambda que se invoca al hacer clic en este elemento, recibiendo el [Podcast] seleccionado.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun PodcastListItem(podcast: Podcast, onPodcastSelected: (Podcast) -> Unit) {
    Log.d("PodcastListItem", "Mostrando item - Título: ${podcast.title}, Duración: ${podcast.duration}, Identifier: ${podcast.identifier}")

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
            // Imagen del podcast
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = "${podcast.title} cover image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
                // TODO: Añadir una imagen por defecto si no se carga la imagen
            )

            Column(modifier = Modifier.weight(1f)) {
                // Título del podcast
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Duración del podcast
                Text(
                    text = podcast.duration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}