package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Composable para un campo de búsqueda con icono.
 *
 * @param searchText El texto actual en el campo de búsqueda.
 * @param onSearchTextChanged Lambda que se invoca cuando el texto de búsqueda cambia.
 * @param modifier Modificador opcional para aplicar a este Composable.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Definir los colores para OutlinedTextField usando MaterialTheme.colorScheme
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        // Colores del borde
        focusedBorderColor = MaterialTheme.colorScheme.primary, // Borde cuando está enfocado
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant, // Borde cuando no está enfocado
        errorBorderColor = MaterialTheme.colorScheme.error, // Color del borde en estado de error

        // Colores del texto de entrada
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        errorTextColor = MaterialTheme.colorScheme.error,

        // Colores de la etiqueta (label)
        focusedLabelColor = MaterialTheme.colorScheme.primary, // Etiqueta cuando está enfocado
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Etiqueta cuando no está enfocado
        errorLabelColor = MaterialTheme.colorScheme.error,

        // Colores del icono principal (leadingIcon)
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary, // Icono cuando está enfocado
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Icono cuando no está enfocado
        errorLeadingIconColor = MaterialTheme.colorScheme.error,

        // Puedes añadir más si necesitas customizar el trailingIcon, cursor, placeholder, etc.
    )

    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        label = {
            Text("Buscar...")
            // El color del texto de la etiqueta ya lo hemos definido en customTextFieldColors.
            // Si necesitaras un color diferente que no esté en el default, lo pondrías aquí
            // color = MaterialTheme.colorScheme.onSurfaceVariant
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Icono de búsqueda",
                // El tint del icono también se controla por customTextFieldColors.
                // Si necesitaras un tint diferente, lo pondrías aquí
                // tint = MaterialTheme.colorScheme.onSurface
            )
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = customTextFieldColors // Aplicar los colores personalizados
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchBar() {
    // Asegúrate de envolver tu preview con tu tema para que los colores se apliquen
    com.example.paradigmaapp.android.MyApplicationTheme {
        var searchText by remember { mutableStateOf("") }
        SearchBar(searchText = searchText, onSearchTextChanged = { searchText = it })
    }
}