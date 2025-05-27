package com.example.paradigmaapp.android.ui // O tu paquete preferido para componentes UI

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Tus recursos
import com.example.paradigmaapp.android.utils.imageUrlFromDescription // Para extraer la URL de la imagen
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities // Para limpiar el nombre
import com.example.paradigmaapp.model.Programa // Tu modelo de datos Programa

/**
 * Composable para mostrar un item de programa en un formato de cuadrícula (imagen arriba, título abajo).
 * Diseñado para ser usado en LazyVerticalGrid en HomeScreen.
 *
 * @param programa El objeto [Programa] a mostrar.
 * @param onClicked Lambda que se invoca al hacer clic en el item.
 * @param modifier Modificador opcional para aplicar a este Composable.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card y otros componentes de Material3
@Composable
fun ProgramaListItem( // Nombre original mantenido, pero con diseño de GridItem
    programa: Programa,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClicked, // Acción al hacer clic en la tarjeta
        modifier = modifier
            .fillMaxWidth()
        ,
        shape = RoundedCornerShape(12.dp), // Esquinas redondeadas para la Card
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Elevación para sombra
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Color de fondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // Centra el texto horizontalmente
        ) {
            val imageUrl = programa.imageUrl
            // Imagen del programa
            AsyncImage(
                model = imageUrl,
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}", // Descripción para accesibilidad
                modifier = Modifier
                    .fillMaxWidth() // La imagen ocupa todo el ancho de la Card
                    .aspectRatio(1f) // Proporción imagen
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), // Redondea solo las esquinas superiores de la imagen
                // para que coincida con la Card si la Card no tiene padding superior.
                contentScale = ContentScale.Crop, // Escala la imagen para llenar el espacio y recorta el exceso
                error = painterResource(R.mipmap.logo_foreground), // Imagen a mostrar si la carga falla
                placeholder = painterResource(R.mipmap.logo_foreground) // Imagen a mostrar mientras carga
            )

            // Espacio entre la imagen y el título
            Spacer(modifier = Modifier.height(8.dp))

            // Nombre del Programa
            Text(
                text = programa.name.unescapeHtmlEntities(), // Nombre del programa, limpiando entidades HTML
                style = MaterialTheme.typography.titleSmall, // Estilo del texto para el título
                fontWeight = FontWeight.SemiBold, // Peso de la fuente
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Color del texto
                textAlign = TextAlign.Center, // Alineación del texto
                maxLines = 2, // Permitir hasta dos líneas para el título
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp) // Padding horizontal para el texto
                    .padding(bottom = 8.dp) // Padding inferior para separar del borde de la Card
            )
        }
    }
}