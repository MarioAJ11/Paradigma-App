package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Tus recursos
import com.example.paradigmaapp.android.utils.extractTextFromHtml // Importa desde utils
import com.example.paradigmaapp.android.utils.imageUrlFromDescription // Importa desde utils
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities // Importa desde utils
import com.example.paradigmaapp.model.Programa

/**
 * Composable para la cabecera de la pantalla de detalles del programa.
 * Muestra la imagen, título y descripción del programa.
 *
 * @param programa El objeto [Programa] a mostrar.
 * @param modifier Modificador opcional para este Composable.
 * @author Mario Alguacil Juárez
 */
@Composable
fun ProgramaHeader(
    programa: Programa,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier // Usa el modifier pasado
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top, // Alinear al tope
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = programa.imageUrlFromDescription(), // Usa la función de extensión
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground), // Placeholder de tus recursos
                placeholder = painterResource(R.mipmap.logo_foreground)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = programa.name.unescapeHtmlEntities(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically) // Centra el título con la imagen
            )
        }
        // Mostrar descripción si existe y no está en blanco
        programa.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = desc.extractTextFromHtml().unescapeHtmlEntities(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}