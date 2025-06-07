package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp


/**
 * Composable auxiliar y privado que dibuja una barra de progreso personalizada y manejable.
 * No se muestra si el contenido es un stream en vivo.
 *
 * @param isLiveStream Si es `true`, la barra de progreso no se dibuja.
 * @param progress El progreso actual de la reproducción (0.0 a 1.0).
 * @param circlePosition La posición del círculo de arrastre.
 * @param showCircle `true` si el círculo de arrastre debe ser visible.
 * @param onProgressChange Callback que notifica un cambio en el progreso.
 * @param onDragStateChange Callback que notifica si el usuario está arrastrando la barra.
 */
@Composable
fun AudioProgressBar(
    isLiveStream: Boolean,
    progress: Float,
    circlePosition: Float,
    showCircle: Boolean,
    onProgressChange: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Si es un stream, no mostramos barra de progreso. Solo un espaciador para mantener el layout.
    if (isLiveStream) {
        Spacer(modifier = modifier.fillMaxWidth().height(8.dp))
        return
    }

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(surfaceVariantColor.copy(alpha = 0.3f))
            // El modificador pointerInput detecta gestos de arrastre horizontal.
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true) // Notifica que el arrastre ha comenzado.
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume() // Consume el evento para que no se propague.
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                    },
                    onDragEnd = { onDragStateChange(false) }, // Notifica que el arrastre ha terminado.
                    onDragCancel = { onDragStateChange(false) }
                )
            }
    ) {
        // Usamos un Canvas para dibujar la barra de progreso de forma personalizada.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressWidth = size.width * progress
            // Dibuja la línea de progreso.
            drawLine(secondaryColor, start = Offset.Zero.copy(y = center.y), end = Offset(progressWidth, center.y), strokeWidth = size.height)
            // Dibuja el círculo de arrastre si es visible.
            if (showCircle) {
                val circleX = (size.width * circlePosition).coerceIn(0f, size.width)
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = 10.dp.toPx(), center = Offset(circleX, center.y))
                drawCircle(onPrimaryColor, radius = 10.dp.toPx(), center = Offset(circleX, center.y), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}