package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Un Composable para mostrar un mensaje de error con un icono opcional y un botón de reintentar.
 *
 * @param message El mensaje de error a mostrar.
 * @param errorType Para determinar el icono y, potencialmente, el mensaje si no se provee uno.
 * @param onRetry La acción a ejecutar cuando se hace clic en el botón de reintentar.
 * Si es nulo, el botón no se muestra.
 * @param modifier Modificador para el contenedor Box.
 */
@Composable
fun ErrorView(
    message: String,
    errorType: ErrorType = ErrorType.GENERAL_SERVER_ERROR, // Por defecto, error general
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val displayMessage: String

    when (errorType) {
        ErrorType.NO_INTERNET -> {
            icon = Icons.Filled.CloudOff
            displayMessage = message // Usar el mensaje pasado, que debería ser específico de "no internet"
        }
        ErrorType.GENERAL_SERVER_ERROR, ErrorType.NO_RESULTS -> { // NO_RESULTS también puede usar el icono de error general
            icon = Icons.Filled.ErrorOutline
            displayMessage = message
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Icono de error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = displayMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

enum class ErrorType {
    NO_INTERNET,
    GENERAL_SERVER_ERROR,
    NO_RESULTS // Para cuando una búsqueda o filtro no devuelve nada, pero no es un error de sistema/red
}