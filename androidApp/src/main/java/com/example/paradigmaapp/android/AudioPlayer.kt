package com.example.paradigmaapp.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Composable que representa el reproductor de audio con controles básicos.
 * Utiliza ExoPlayer para la reproducción.
 *
 * @param player Instancia de ExoPlayer que gestiona la reproducción.
 * @param onPlayPauseClick Lambda que se ejecuta al hacer clic en el botón Play/Pause.
 * @param progress Progreso actual de la reproducción (valor entre 0.0 y 1.0).
 * @param onProgressChange Lambda que se ejecuta cuando el usuario cambia el progreso arrastrando el Slider.
 * @param modifier Modificadores para aplicar a la composición.
 */
@Composable
fun AudioPlayer(
    player: ExoPlayer,
    onPlayPauseClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado para controlar la visibilidad de los controles de volumen.
    var showVolumeControls by remember { mutableStateOf(false) }
    // Estado para almacenar el volumen actual del reproductor.
    // Utilizamos mutableFloatStateOf para estados primitivos de tipo Float.
    var currentVolume by remember { mutableFloatStateOf(player.volume) }

    // Card que envuelve el reproductor, dándole un fondo y forma.
    Card(
        modifier = modifier.fillMaxWidth(), // Asegura que el Card ocupe todo el ancho disponible.
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)), // Color de fondo del Card (Amarillo).
        shape = RoundedCornerShape(12.dp) // Esquinas redondeadas para el Card.
    ) {
        // Columna principal que organiza los elementos del reproductor verticalmente.
        Column(
            modifier = Modifier.padding(1.dp), // Pequeño padding interno.
            verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre los elementos de la columna.
        ) {
            // Fila de controles principales: Botón Play/Pause, Slider de progreso y Botón de volumen.
            Row(
                verticalAlignment = Alignment.CenterVertically, // Alinea verticalmente los elementos al centro de la fila.
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio horizontal entre los elementos.
            ) {
                // Botón Play/Pause.
                IconButton(
                    onClick = onPlayPauseClick, // Asigna la acción onClick proporcionada.
                    modifier = Modifier.size(48.dp) // Tamaño fijo para el botón.
                ) {
                    // Imagen del botón, cambia entre play y pause según el estado del reproductor.
                    Image(
                        painter = painterResource( // Carga la imagen desde los recursos.
                            id = if (player.isPlaying) // Condición: si está reproduciendo, muestra pause.
                                R.mipmap.pause
                            else // De lo contrario, muestra play.
                                R.mipmap.play
                        ),
                        contentDescription = if (player.isPlaying) "Pause" else "Play", // Descripción de accesibilidad.
                        modifier = Modifier.size(32.dp) // Tamaño de la imagen dentro del botón.
                    )
                }

                // Slider para mostrar y controlar el progreso de la reproducción.
                Slider(
                    value = progress, // Valor actual del slider (proviene del estado).
                    onValueChange = onProgressChange, // Lambda ejecutada cuando cambia el valor (al arrastrar).
                    valueRange = 0f..1f, // Rango de valores del slider (de 0 a 1, representando el 0% al 100%).
                    modifier = Modifier
                        .weight(1f) // Hace que el Slider ocupe todo el espacio horizontal disponible.
                        .height(40.dp), // Altura fija del Slider.
                    colors = SliderDefaults.colors( // Colores personalizados para el Slider.
                        thumbColor = Color(0xFF555555), // Color del "pulgar" (la bolita que arrastras).
                        activeTrackColor = Color(0xFF555555), // Color de la barra activa (lo que ya se ha reproducido).
                        inactiveTrackColor = Color(0xFFFFFFFF) // Color de la barra inactiva (lo que falta por reproducir).
                    )
                )

                // Texto que muestra el tiempo actual y la duración total del podcast.
//                Text(
//                    text = "${formatTime(player.currentPosition)} / ${formatTime(player.duration)}", // Formatea y muestra el tiempo.
//                    color = Color(0xFF555555), // Color del texto.
//                    fontSize = 9.sp, // Tamaño de la fuente.
//                    modifier = Modifier.width(60.dp) // Ancho fijo para el texto del tiempo.
//                )

                // Botón para mostrar/ocultar los controles de volumen.
                IconButton(
                    onClick = { showVolumeControls = !showVolumeControls }, // Cambia el estado de visibilidad.
                    modifier = Modifier.size(40.dp) // Tamaño fijo para el botón.
                ) {
                    // Imagen del icono de volumen.
                    Image(
                        painter = painterResource(R.mipmap.volume),
                        contentDescription = "Volume Control",
                        modifier = Modifier.size(28.dp) // Tamaño de la imagen dentro del botón.
                    )
                }
            }

            // Controles de volumen (Slider) - se muestran condicionalmente.
            if (showVolumeControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Alinea verticalmente al centro.
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Espacio horizontal entre elementos.
                    modifier = Modifier.padding(top = 8.dp) // Padding superior cuando se muestran.
                ) {
                    // Icono de volumen bajo.
                    Image(
                        painter = painterResource(R.mipmap.volume_down),
                        contentDescription = "Low volume",
                        modifier = Modifier.size(28.dp) // Tamaño del icono.
                    )

                    // Slider para controlar el volumen.
                    Slider(
                        value = currentVolume, // Valor actual del volumen.
                        onValueChange = { newVolume ->
                            currentVolume = newVolume // Actualiza el estado del volumen en la UI.
                            player.volume = newVolume // Actualiza el volumen en el ExoPlayer.
                        },
                        valueRange = 0f..1f, // Rango de volumen (de 0 a 1).
                        modifier = Modifier
                            .weight(1f) // Ocupa el espacio disponible.
                            .height(40.dp), // Altura aumentada para mejor interacción.
                        colors = SliderDefaults.colors( // Colores personalizados.
                            thumbColor = Color(0xFF555555),
                            activeTrackColor = Color(0xFF555555),
                            inactiveTrackColor = Color(0xFFFFFFFF)
                        )
                    )

                    // Icono de volumen alto.
                    Image(
                        painter = painterResource(R.mipmap.volume),
                        contentDescription = "High volume",
                        modifier = Modifier.size(28.dp) // Tamaño del icono.
                    )
                }
            }
        }
    }
}

/**
 * Función de utilidad para formatear milisegundos a un string de tiempo (MM:SS).
 *
 * @param millis El tiempo en milisegundos.
 * @return String formateado como "MM:SS".
 */
fun formatTime(millis: Long): String {
    if (millis < 0) return "00:00" // Manejo de caso con duración negativa.
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}