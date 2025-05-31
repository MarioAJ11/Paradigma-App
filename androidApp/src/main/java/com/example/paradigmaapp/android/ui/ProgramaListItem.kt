package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.model.Programa

/**
 * Composable para mostrar un ítem de programa en mi cuadrícula.
 * Muestra una imagen del programa y su título debajo.
 * Está diseñado para tener una altura consistente en la sección del título,
 * basándose en un número máximo de líneas predefinido (actualmente 2).
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramaListItem(
    programa: Programa,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClicked,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // La columna ocupa toda la tarjeta
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageUrl = programa.imageUrl
            // Imagen del programa
            AsyncImage(
                model = imageUrl,
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Imagen cuadrada
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground),
                placeholder = painterResource(R.mipmap.logo_foreground)
            )

            // Espacio entre la imagen y el área del título.
            Spacer(modifier = Modifier.height(8.dp))

            // Contenedor para el título, con altura mínima para X líneas.
            // Calculamos una altura mínima aproximada para 2 líneas de texto.
            // Esto puede necesitar ajuste fino basado en tu lineHeight y fontSize exactos.
            val typography = MaterialTheme.typography.titleSmall
            val twoLinesHeight = with(LocalDensity.current) {
                // (fontSize + fontPadding) * numLines * factorDeCorreccion
                // El lineHeight del titleSmall ya debería incluir el padding.
                // Si typography.lineHeight no está especificado en sp, podríamos usar fontSize.
                // Por simplicidad, asumimos que titleSmall.fontSize es la base.
                val fontSizeInDp = typography.fontSize.toDp()
                // Un lineHeight típico es ~1.2 a 1.5 veces el fontSize.
                // Si titleSmall tiene un lineHeight definido, sería mejor usarlo.
                // Vamos a estimar con un factor. (fontSize * 1.3 (lineHeight) * 2 (lines))
                // o más simple, si una línea son unos 18-20dp, dos líneas son 36-40dp.
                // Para titleSmall (14.sp por defecto en M3), con lineHeight (20.sp por defecto)
                // 20.sp * 2 líneas = 40.sp.
                (typography.lineHeight * 2).toDp() // Esto es lo más correcto si lineHeight está en sp
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp) // Padding inferior para separar del borde de la Card
                    // Establezco una altura mínima para el área del título.
                    // Esto asegura que incluso los títulos de una línea reserven espacio como si tuvieran dos,
                    // ayudando a alinear las tarjetas en una fila de la cuadrícula.
                    .heightIn(min = twoLinesHeight), // Altura mínima para el contenedor del título
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Centrar el texto verticalmente si ocupa menos de la altura mínima
            ) {
                Text(
                    text = programa.name.unescapeHtmlEntities(),
                    style = typography, // Usar la tipografía definida arriba
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2, // Limito el título a un máximo de 3 líneas.
                    overflow = TextOverflow.Ellipsis, // Si excede, se trunca con puntos suspensivos.
                    // No necesito un modifier aquí si el Column padre maneja el padding y la altura.
                )
            }
        }
    }
}