package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Dibuja una barra de progreso personalizada y manejable, con un círculo siempre visible.
 *
 * @param isLiveStream Si es `true`, la barra no se dibuja.
 * @param progress El progreso real de la reproducción (para la parte coloreada de la barra).
 * @param dragPosition La posición del círculo de arrastre (0.0 a 1.0).
 * @param onDragChange Callback que se invoca continuamente mientras el usuario arrastra.
 * @param onDragEnd Callback que se invoca cuando el usuario suelta el círculo.
 */
@Composable
fun AudioProgressBar(
    isLiveStream: Boolean,
    progress: Float,
    dragPosition: Float,
    onDragChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLiveStream) {
        return
    }

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp) // Altura suficiente para que el gesto de arrastre sea cómodo
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        // Al empezar a arrastrar, actualizamos la posición
                        val newPosition = (offset.x / size.width).coerceIn(0f, 1f)
                        onDragChange(newPosition)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                        onDragChange(newPosition)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        // Usamos un Canvas para dibujar la barra y el círculo de forma personalizada.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = 4.dp.toPx()
            // Dibuja la barra de fondo
            drawLine(surfaceVariantColor.copy(alpha = 0.3f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = barHeight)
            // Dibuja la parte de la barra que ya se ha reproducido
            val progressWidth = size.width * progress
            drawLine(secondaryColor, start = Offset.Zero.copy(y = center.y), end = Offset(progressWidth, center.y), strokeWidth = barHeight)

            // Dibuja el círculo en la posición de arrastre (o en la de reproducción si no se arrastra).
            val circleX = (size.width * dragPosition).coerceIn(0f, size.width)
            drawCircle(color = onPrimaryColor, radius = 6.dp.toPx(), center = Offset(circleX, center.y))
        }
    }
}