package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Muestra una fila de información con un icono y un texto descriptivo.
 * Está diseñado para ser utilizado en pantallas de bienvenida o secciones de ayuda
 * para dar consejos o información contextual al usuario.
 *
 * @author Mario Alguacil Juárez
 * @param modifier Un [Modifier] opcional para aplicar al contenedor [Row] principal.
 */
@Composable
fun InfoRow(modifier: Modifier = Modifier) {
    // Row es un layout que organiza a sus hijos en una secuencia horizontal.
    Row(
        // El modifier se aplica al Row para darle estilo y comportamiento.
        modifier = modifier
            .fillMaxWidth() // Ocupa todo el ancho disponible.
            .padding(8.dp), // Añade un espacio de 8dp en todos los lados.
        // Alinea verticalmente los elementos (Icono y Texto) al centro del Row.
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Muestra un icono de 'Información' estándar de Material Design.
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Información", // Texto para accesibilidad.
            // Tinta el icono con el color secundario del tema actual para un estilo consistente.
            tint = MaterialTheme.colorScheme.secondary
        )
        // Crea un espacio horizontal fijo entre el icono y el texto.
        Spacer(Modifier.width(8.dp))

        // Muestra el texto informativo.
        Text(
            text = "En la sección de 'Ajustes' encontrarás una guía con todas las funcionalidades de la aplicación.",
            // Aplica un estilo de texto predefinido del tema (cuerpo de texto mediano).
            style = MaterialTheme.typography.bodyMedium,
            // Aplica el color de texto secundario del tema.
            color = MaterialTheme.colorScheme.secondary
        )
    }
}