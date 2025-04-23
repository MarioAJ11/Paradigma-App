package com.example.paradigmaapp.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Composable que muestra una lista de podcasts en un formato similar a una lista de Spotify.
 * Cada elemento muestra una imagen y el título del podcast.
 *
 * @param podcasts Lista de objetos [Podcast] a mostrar.
 * @param onPodcastSelected Lambda que se ejecuta cuando se selecciona un podcast de la lista.
 */
@Composable
fun PodcastList(podcasts: List<Podcast>, onPodcastSelected: (Podcast) -> Unit) {
    // LazyColumn es eficiente para mostrar listas largas, solo compone los elementos visibles.
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Itera sobre la lista de podcasts y crea un elemento de lista para cada uno.
        items(podcasts) { podcast ->
            // Card que representa cada elemento de podcast en la lista.
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF747272)), // Color de fondo del Card.
                modifier = Modifier
                    .fillMaxWidth() // Ocupa todo el ancho del contenedor.
                    .padding(vertical = 4.dp, horizontal = 8.dp) // Padding alrededor de cada Card.
                    .clickable { onPodcastSelected(podcast) } // Hace que el Card sea clickeable.
            ) {
                // Fila dentro del Card para mostrar la imagen y el texto horizontalmente.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp), // Padding dentro del Card.
                    verticalAlignment = Alignment.CenterVertically, // Alinea verticalmente al centro.
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio horizontal entre imagen y texto.
                ) {
                    // Carga y muestra la imagen del podcast desde la URL usando Coil.
                    // AsyncImage maneja la carga asíncrona y el placeholder/error.
                    AsyncImage(
                        model = podcast.imageUrl, // URL de la imagen.
                        contentDescription = "${podcast.title} cover image", // Descripción para accesibilidad.
                        modifier = Modifier
                            .size(60.dp), // Tamaño fijo para la imagen.
                        // Puedes añadir un .clip(RoundedCornerShape(4.dp)) si quieres esquinas redondeadas en la imagen.
                        contentScale = ContentScale.Crop // Escala la imagen para llenar el espacio sin distorsión.
                        // Puedes añadir 'placeholder' y 'error' parámetros para iconos de carga/error.
                    )

                    // Texto del título del podcast.
                    Text(
                        text = podcast.title, // Muestra el título del podcast.
                        color = textColor, // Color del texto (definido en PodcastScreen).
                        fontSize = 18.sp, // Tamaño de la fuente.
                        fontWeight = FontWeight.Medium, // Peso de la fuente.
                        modifier = Modifier.weight(1f) // Permite que el texto ocupe el espacio restante.
                    )
                }
            }
        }
    }
}