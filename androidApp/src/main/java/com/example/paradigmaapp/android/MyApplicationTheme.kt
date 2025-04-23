package com.example.paradigmaapp.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Define el esquema de colores para el tema oscuro.
 * - primary: Color principal para elementos destacados.
 * - secondary: Color secundario para elementos complementarios.
 * - background: Color de fondo general de la pantalla.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700), // Amarillo oro
    secondary = Color(0xFF03DAC6), // Cian (ejemplo, puedes ajustarlo)
    background = Color.Black // Fondo negro
)

/**
 * Define el esquema de colores para el tema claro.
 * - primary: Color principal.
 * - secondary: Color secundario.
 * - background: Color de fondo general.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFD700), // Amarillo oro
    secondary = Color(0xFF03DAC6), // Cian (ejemplo, puedes ajustarlo)
    background = Color.White // Fondo blanco
)

/**
 * Composable que aplica el tema Material Design 3 a la aplicación.
 * Selecciona automáticamente el tema oscuro o claro según la configuración del sistema,
 * a menos que se especifique lo contrario.
 *
 * @param darkTheme Booleano que indica si se debe usar el tema oscuro. Por defecto, usa la configuración del sistema.
 * @param content El contenido Composable al que se aplicará el tema.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Determina si el sistema está en tema oscuro.
    content: @Composable () -> Unit // El bloque de Composables a tematizar.
) {
    // Selecciona el esquema de colores basado en si darkTheme es true.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Aplica el tema MaterialTheme con el esquema de colores seleccionado.
    MaterialTheme(
        colorScheme = colorScheme, // Esquema de colores a aplicar.
        content = content // Contenido al que se aplica el tema.
        // Puedes añadir typography y shapes aquí si los defines en tu tema.
    )
}