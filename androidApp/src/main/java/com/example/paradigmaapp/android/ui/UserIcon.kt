package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.tooling.preview.Preview

/**
 * Composable para mostrar un icono de usuario circular y clickeable.
 * Al hacer clic, invoca la acción proporcionada.
 *
 * @param modifier Modificador opcional para aplicar a este Composable.
 * @param onClick Lambda que se invoca al hacer clic en el icono. Se espera que sea no nulo.
 */
@Composable
fun UserIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit // Cambiado a un lambda no nulo, el clic es la función principal
) {
    // Usamos Surface para crear el círculo de color primario como fondo del botón.
    // IconButton se coloca encima para manejar el clic y mostrar el icono.
    Surface(
        shape = CircleShape, // Forma circular para la superficie
        color = MaterialTheme.colorScheme.primary, // Color de fondo (el círculo)
        modifier = modifier.size(48.dp) // Tamaño total del círculo
    ) {
        // IconButton para el icono clickeable
        IconButton(
            onClick = onClick, // Pasa la acción del clic
            // El modificador del IconButton puede ajustar el relleno interno si es necesario
            modifier = Modifier.fillMaxSize() // Hacer que el IconButton llene la Surface
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Icono de usuario para ajustes o perfil",
                tint = MaterialTheme.colorScheme.onPrimary // Color del icono, contrastando con el color primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserIcon() {
    // En el preview, simplemente pasamos un lambda vacío para el clic
    UserIcon(onClick = {})
}