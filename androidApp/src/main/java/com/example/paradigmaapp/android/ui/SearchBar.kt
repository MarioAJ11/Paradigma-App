package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Composable para un campo de búsqueda con icono y opción de limpiar.
 *
 * @param searchText El texto actual en el campo de búsqueda.
 * @param onSearchTextChanged Lambda que se invoca cuando el texto de búsqueda cambia.
 * @param onClearSearch Lambda opcional que se invoca para limpiar el campo de búsqueda.
 * @param modifier Modificador opcional para aplicar a este Composable.
 * @param label El texto para la etiqueta del campo de búsqueda.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onClearSearch: (() -> Unit)? = null, // Opcional para el botón de limpiar
    modifier: Modifier = Modifier,
    label: String = "Buscar..." // Label personalizable
) {
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        errorTextColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLeadingIconColor = MaterialTheme.colorScheme.error,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Color para el icono de limpiar
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Color para el icono de limpiar
        cursorColor = MaterialTheme.colorScheme.primary
    )

    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Icono de búsqueda"
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty() && onClearSearch != null) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Limpiar búsqueda"
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp), // Esquinas más redondeadas como en M3
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp), // Altura mínima estándar
        colors = customTextFieldColors,
        singleLine = true
    )
}