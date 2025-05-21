package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.tooling.preview.Preview
import com.example.paradigmaapp.android.MyApplicationTheme // Importa tu tema para el preview

/**
 * Composable que muestra un diálogo simple para ajustes, ahora con un enlace a la web y control de streaming.
 *
 * @param onDismissRequest Lambda que se invoca cuando se cierra el diálogo.
 * @param isStreamActive Indica si el streaming está activo o no.
 * @param onStreamActiveChanged Lambda que se invoca cuando el estado de streaming cambia.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun SettingsScreen(
    onDismissRequest: () -> Unit,
    isStreamActive: Boolean,
    onStreamActiveChanged: (Boolean) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            // El color del texto del título por defecto será MaterialTheme.colorScheme.onSurface
            Text(text = "Ajustes")
        },
        text = {
            Column {
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://paradigmamedia.org/")
                        onDismissRequest() // Cierra el dialogo al pulsar en el enlace
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Visitar web de Paradigma Media")
                }
                // Añade aquí otras opciones de ajustes si es necesario
                // Ejemplo de un control para activar/desactivar el streaming
                TextButton(
                    onClick = { onStreamActiveChanged(!isStreamActive) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isStreamActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary // Rojo si activo, primary si inactivo
                    )
                ) {
                    // El color del texto aquí se verá afectado por el contentColor de arriba.
                    Text(if (isStreamActive) "Desactivar Streaming" else "Activar Streaming")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    MyApplicationTheme {
        var isStreamActiveState by remember { mutableStateOf(false) }
        SettingsScreen(
            onDismissRequest = {},
            isStreamActive = isStreamActiveState,
            onStreamActiveChanged = { isStreamActiveState = it }
        )
    }
}