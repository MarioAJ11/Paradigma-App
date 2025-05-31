package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R

/**
 * Composable que proporciona un control para ajustar el volumen de audio.
 *
 * @param onVolumeChanged Lambda que se invoca cuando el valor del volumen cambia a través del slider.
 * Recibe como parámetro el nuevo valor del volumen (un [Float] entre 0.0 y 1.0).
 * @param currentVolume El valor actual del volumen de reproducción (un [Float] entre 0.0 y 1.0).
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun VolumeControl(
    onVolumeChanged: (Float) -> Unit,
    currentVolume: Float
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Control de Volumen",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Slider con iconos
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.volume_down),
                contentDescription = "Volumen Bajo",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    onVolumeChanged(newVolume)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary, // Color del pulgar
                    activeTrackColor = MaterialTheme.colorScheme.primary, // Color de la pista activa
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) // Color de la pista inactiva
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.mipmap.volume),
                contentDescription = "Volumen Alto",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "Volumen: ${(currentVolume * 100).toInt()}%",
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}