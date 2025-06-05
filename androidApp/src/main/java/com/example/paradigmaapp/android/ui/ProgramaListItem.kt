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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Recursos de Android
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities // Utilidad para decodificar HTML
import com.example.paradigmaapp.model.Programa // Modelo de Programa del módulo shared

/**
 * Un Composable que representa un ítem individual de [Programa] en una lista o cuadrícula.
 * Muestra la imagen del programa y su título. Está diseñado para tener una altura
 * consistente en el área del título, ayudando a la alineación visual cuando se muestra en una cuadrícula.
 *
 * @param programa El objeto [Programa] cuyos datos se mostrarán.
 * @param onClicked Lambda que se invoca cuando el usuario hace clic en el ítem del programa.
 * @param modifier Modificador opcional para aplicar al [Card] principal.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card en Material 3
@Composable
fun ProgramaListItem(
    programa: Programa,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClicked,
        modifier = modifier.fillMaxWidth(), // La tarjeta ocupa el ancho disponible en su celda/columna
        shape = RoundedCornerShape(12.dp), // Esquinas redondeadas para la tarjeta
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Elevación sutil
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // La columna interna ocupa toda la tarjeta
            horizontalAlignment = Alignment.CenterHorizontally // Centra la imagen y el texto horizontalmente
        ) {
            AsyncImage(
                model = programa.imageUrl, // URL de la imagen del programa
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Mantiene la imagen cuadrada
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), // Redondea solo esquinas superiores de la imagen
                contentScale = ContentScale.Crop, // Escala la imagen para llenar el espacio y recorta si es necesario
                error = painterResource(R.mipmap.logo_foreground), // Imagen de fallback
                placeholder = painterResource(R.mipmap.logo_foreground) // Imagen mientras carga
            )

            Spacer(modifier = Modifier.height(8.dp)) // Espacio entre la imagen y el título

            // Contenedor para el título, con altura mínima para asegurar consistencia visual en cuadrículas.
            val typography = MaterialTheme.typography.titleSmall
            // Calcula la altura aproximada para dos líneas de texto del estilo titleSmall.
            val twoLinesHeight: Dp = with(LocalDensity.current) {
                // (lineHeight de titleSmall) * 2 líneas.
                // Se asume que typography.lineHeight está definido en sp y se convierte a Dp.
                (typography.lineHeight * 2).toDp() //
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp) // Padding horizontal para el texto del título
                    .padding(bottom = 8.dp) // Padding inferior para el texto del título
                    .heightIn(min = twoLinesHeight), // Altura mínima para el área del título
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Centra el texto verticalmente si ocupa menos de la altura mínima
            ) {
                Text(
                    text = programa.name.unescapeHtmlEntities(), // Nombre del programa decodificado
                    style = typography,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, // Texto del título centrado
                    maxLines = 2, // Máximo dos líneas para el título
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}