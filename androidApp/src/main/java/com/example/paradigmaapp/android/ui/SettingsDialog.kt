package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/**
 * Composable que muestra un diálogo simple para ajustes, ahora con un enlace a la web.
 */
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Ajustes")
        },
        text = {
            Column {
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://paradigmamedia.org/")
                        // onDismissRequest() // Opcional: cerrar el diálogo después de abrir el enlace
                    }
                ) {
                    Text("Visitar web de Paradigma Media")
                }
                // Añade aquí otras opciones de ajustes si es necesario
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar")
            }
        }
    )
}