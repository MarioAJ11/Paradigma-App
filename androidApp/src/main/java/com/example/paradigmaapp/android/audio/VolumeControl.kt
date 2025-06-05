package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R

/**
 * Composable que proporciona una interfaz de usuario para ajustar el volumen del audio.
 * Incluye un [Slider] y un indicador textual del porcentaje de volumen.
 * Se muestra típicamente en un BottomSheet.
 *
 * @param currentVolume El valor actual del volumen, un [Float] entre 0.0 (silencio) y 1.0 (máximo).
 * Este valor se utiliza para establecer la posición inicial del slider.
 * @param onVolumeChanged Lambda que se invoca cuando el usuario interactúa con el [Slider].
 * Recibe el nuevo valor del volumen ([Float] entre 0.0 y 1.0)
 * seleccionado por el usuario.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun VolumeControl(
    currentVolume: Float,
    onVolumeChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp) // Padding alrededor del contenido del control de volumen
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio vertical entre elementos
    ) {
        Text(
            text = "Control de Volumen",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground // Color del texto del título
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.volume_down), // Icono de volumen bajo
                contentDescription = "Volumen Bajo",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface // Tinte del icono
            )
            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre icono y slider
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    onVolumeChanged(newVolume.coerceIn(0f, 1f)) // Asegura que el valor esté en el rango
                },
                valueRange = 0f..1f, // Rango del slider de 0% a 100%
                modifier = Modifier.weight(1f), // El slider ocupa el espacio restante
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre slider e icono
            Icon(
                painter = painterResource(id = R.mipmap.volume), // Icono de volumen alto
                contentDescription = "Volumen Alto",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface // Tinte del icono
            )
        }
        Text(
            text = "Volumen: ${(currentVolume * 100).toInt()}%", // Muestra el volumen como porcentaje
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}