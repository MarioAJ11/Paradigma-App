package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.R

/**
 * Composable que proporciona controles para ajustar el volumen de audio y seleccionar dispositivos de salida.
 *
 * Presenta un slider para controlar el volumen de reproducción y una lista de dispositivos
 * Bluetooth disponibles para la selección como salida de audio.
 *
 * @param player El [ExoPlayer] asociado para acceder y modificar el volumen. Puede ser nulo.
 * @param onVolumeChanged Lambda que se invoca cuando el valor del volumen cambia a través del slider.
 * Recibe como parámetro el nuevo valor del volumen (un [Float] entre 0.0 y 1.0).
 * @param currentVolume El valor actual del volumen de reproducción (un [Float] entre 0.0 y 1.0).
 * @param onBluetoothDeviceSelected Lambda opcional que se invoca cuando el usuario selecciona un dispositivo
 * Bluetooth de la lista. Recibe como parámetro el nombre del dispositivo seleccionado (un [String]).
 * Por defecto, no realiza ninguna acción.
 * @param availableBluetoothDevices Lista de nombres de dispositivos Bluetooth disponibles para la conexión (una [List] de [String]).
 * Por defecto, es una lista vacía.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun VolumeControl(
    player: ExoPlayer?,
    onVolumeChanged: (Float) -> Unit,
    currentVolume: Float,
    onBluetoothDeviceSelected: (String) -> Unit = {},
    availableBluetoothDevices: List<String> = emptyList()
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
                    thumbColor = Color.Gray,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
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

        // Sección de Dispositivos
        Text(
            "Dispositivos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (availableBluetoothDevices.isNotEmpty()) {
            // Mostrar dispositivos Bluetooth como lista
            Column(horizontalAlignment = Alignment.Start) {
                availableBluetoothDevices.forEach { deviceName ->
                    Button(
                        onClick = { onBluetoothDeviceSelected(deviceName) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(deviceName)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Text(
                "No hay dispositivos conectados en este momento",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}