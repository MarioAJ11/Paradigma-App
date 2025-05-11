package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

/**
 * Composable para un campo de búsqueda con icono.
 *
 * @param searchText El texto actual en el campo de búsqueda.
 * @param onSearchTextChanged Lambda que se invoca cuando el texto de búsqueda cambia.
 * @param modifier Modificador opcional para aplicar a este Composable.
 */
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        label = { Text("Buscar...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Icono de búsqueda"
            )
        },
        modifier = modifier
            .fillMaxWidth() // Esto probablemente se sobrescribirá al usarlo en una Row,
            // pero es un buen valor predeterminado si se usa solo.
            .height(56.dp) // Altura fija
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchBar() {
    var searchText by remember { mutableStateOf("") }
    SearchBar(searchText = searchText, onSearchTextChanged = { searchText = it })
}