package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Un Composable que implementa un campo de búsqueda estilizado.
 * Utiliza un [OutlinedTextField] con un icono de búsqueda principal y un icono
 * opcional para limpiar el texto introducido.
 *
 * @param searchText El texto actual que se muestra en el campo de búsqueda.
 * @param onSearchTextChanged Lambda que se invoca cada vez que el texto en el campo de búsqueda cambia.
 * Recibe la nueva cadena de texto como parámetro.
 * @param onClearSearch Lambda opcional que se invoca cuando el usuario pulsa el icono de limpiar (X).
 * Si es `null`, el icono de limpiar no se muestra.
 * @param modifier Modificador opcional para aplicar al [OutlinedTextField].
 * @param label El texto que se muestra como etiqueta flotante o placeholder del campo de búsqueda.
 * Por defecto es "Buscar...".
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onClearSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    label: String = "Buscar..." //
) {
    // Define colores personalizados para el OutlinedTextField para un control más fino.
    // Estos colores se toman del MaterialTheme.colorScheme para adaptarse al tema actual.
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        // errorBorderColor = MaterialTheme.colorScheme.error, // No se usa explícitamente estado de error aquí
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        // errorTextColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        // errorLabelColor = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        // errorLeadingIconColor = MaterialTheme.colorScheme.error,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    ) //

    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search, // Icono de lupa para búsqueda
                contentDescription = "Icono de búsqueda" // Descripción para accesibilidad
            )
        },
        trailingIcon = {
            // Muestra el icono de limpiar (X) solo si el texto no está vacío y se ha proporcionado la acción onClearSearch.
            if (searchText.isNotEmpty() && onClearSearch != null) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Filled.Close, // Icono de X para limpiar
                        contentDescription = "Limpiar búsqueda" // Descripción para accesibilidad
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp), // Esquinas redondeadas para un estilo moderno (Material 3)
        modifier = modifier
            .fillMaxWidth() // Ocupa todo el ancho disponible
            .heightIn(min = 56.dp), // Altura mínima estándar para campos de texto
        colors = customTextFieldColors, // Aplica los colores personalizados definidos arriba
        singleLine = true // El campo de búsqueda es de una sola línea
    )
}