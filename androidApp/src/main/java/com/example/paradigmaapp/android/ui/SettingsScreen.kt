package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/**
 * Composable que muestra un diálogo simple para ajustes, ahora con un enlace a la web y control de streaming.
 */
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    isStreamActive: Boolean,
    onStreamActiveChanged: (Boolean) -> Unit
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
                // Ejemplo de un control para activar/desactivar el streaming
                TextButton(
                    onClick = { onStreamActiveChanged(!isStreamActive) }
                ) {
                    Text(if (isStreamActive) "Desactivar Streaming" else "Activar Streaming")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar")
            }
        }
    )
}