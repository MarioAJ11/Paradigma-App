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
 */
@Composable
fun PodcastList(
    podcasts: List<Podcast>,
    onPodcastSelected: (Podcast) -> Unit,
    // *** Añade esta línea: Define un parámetro 'modifier' con un valor por defecto ***
    modifier: Modifier = Modifier
) {
    LazyColumn(
        // *** Modifica esta línea: Aplica el 'modifier' que se pasa como parámetro ***
        modifier = modifier // Usa el modificador que recibe la función
            .fillMaxWidth(), // Combina el modificador pasado con fillMaxWidth()
        contentPadding = PaddingValues(vertical = 8.dp) // Añade un padding vertical a la lista.
    ) {
        // Utiliza la función 'items' para iterar eficientemente sobre la lista de podcasts.
        items(podcasts) { podcast ->
            // Para cada podcast, renderiza el composable [PodcastListItem].
            PodcastListItem(podcast = podcast, onPodcastSelected = onPodcastSelected)
        }
    }
}