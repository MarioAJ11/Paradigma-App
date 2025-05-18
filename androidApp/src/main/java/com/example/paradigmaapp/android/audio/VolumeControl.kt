package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.R

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
        Text("Control de Volumen", style = MaterialTheme.typography.headlineSmall)

        // Slider con iconos
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.volume_down),
                contentDescription = "Volumen Bajo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    onVolumeChanged(newVolume)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.mipmap.volume),
                contentDescription = "Volumen Alto",
                modifier = Modifier.size(24.dp)
            )
        }
        Text("Volumen: ${(currentVolume * 100).toInt()}%")

        // Sección de Dispositivos
        Text("Dispositivos", style = MaterialTheme.typography.titleMedium)
        if (availableBluetoothDevices.isNotEmpty()) {
            // Mostrar dispositivos Bluetooth como lista
            Column(horizontalAlignment = Alignment.Start) {
                availableBluetoothDevices.forEach { deviceName ->
                    Button(
                        onClick = { onBluetoothDeviceSelected(deviceName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(deviceName)
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Espacio entre botones
                }
            }
        } else {
            Text("No hay dispositivos conectados en este momento")
        }
    }
}