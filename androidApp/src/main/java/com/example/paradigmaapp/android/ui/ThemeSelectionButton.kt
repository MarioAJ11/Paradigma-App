package com.example.paradigmaapp.android.ui

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Composable que representa un botón de selección de tema en la interfaz de usuario.
 *
 * @param onClick Lambda a ejecutar cuando se pulsa el botón.
 * @param text El texto que mostrará el botón.
 * @param isSelected Booleano que indica si este botón es la opción actualmente seleccionada.
 * @param modifier Modificador para aplicar al botón.
 */
@Composable
fun ThemeSelectionButton(
    onClick: () -> Unit,
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}