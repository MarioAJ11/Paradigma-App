package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Enum para categorizar los tipos de error que [ErrorView] puede mostrar.
 * Ayuda a seleccionar un icono y un mensaje apropiados.
 */
enum class ErrorType {
    /** Indica un problema de conectividad a internet. */
    NO_INTERNET,
    /** Indica un error genérico del servidor o un problema inesperado del backend. */
    GENERAL_SERVER_ERROR,
    /** Indica que una operación (ej. búsqueda, carga de datos) no produjo resultados,
     * pero no es necesariamente un error de sistema o red. */
    NO_RESULTS
}

/**
 * Un Composable reutilizable para mostrar un mensaje de error al usuario.
 * Incluye un icono representativo del tipo de error, un mensaje descriptivo
 * y un botón opcional para reintentar la acción que falló.
 *
 * @param message El mensaje de error específico a mostrar.
 * @param errorType El [ErrorType] que ayuda a determinar el icono.
 * Por defecto es [ErrorType.GENERAL_SERVER_ERROR].
 * @param onRetry Una lambda opcional que se ejecuta cuando el usuario pulsa el botón "Reintentar".
 * Si es `null`, el botón de reintentar no se muestra.
 * @param modifier Un [Modifier] opcional para aplicar al contenedor principal [Box] del [ErrorView].
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun ErrorView(
    message: String,
    errorType: ErrorType = ErrorType.GENERAL_SERVER_ERROR,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    // El mensaje se toma directamente del parámetro `message` para mayor flexibilidad.
    // val displayMessage: String // No es necesario si message siempre se provee.

    icon = when (errorType) {
        ErrorType.NO_INTERNET -> Icons.Filled.CloudOff //
        ErrorType.GENERAL_SERVER_ERROR, ErrorType.NO_RESULTS -> Icons.Filled.ErrorOutline //
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // Padding estándar para el contenido del error
        contentAlignment = Alignment.Center // Centra la columna de error en el Box
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centra los elementos dentro de la columna
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Icono de error", // Descripción para accesibilidad
                modifier = Modifier.size(64.dp), // Tamaño del icono
                tint = MaterialTheme.colorScheme.error // Color del icono, usualmente el color de error del tema
            )
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre el icono y el mensaje
            Text(
                text = message, // Muestra el mensaje proporcionado
                textAlign = TextAlign.Center, // Alineación del texto
                style = MaterialTheme.typography.bodyLarge, // Estilo de texto
                color = MaterialTheme.colorScheme.onSurface // Color del texto
            )
            // Muestra el botón de reintentar solo si la lambda onRetry no es nula
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp)) // Espacio entre el mensaje y el botón
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar") // Icono para el botón
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing)) // Espacio estándar de Material3 para iconos en botones
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}