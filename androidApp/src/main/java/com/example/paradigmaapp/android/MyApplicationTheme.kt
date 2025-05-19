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
 * - surface: Color de superficies como tarjetas, paneles, etc.
 * - onPrimary: Color del texto/iconos sobre el color primario.
 * - onSecondary: Color del texto/iconos sobre el color secundario.
 * - onBackground: Color del texto/iconos sobre el color de fondo.
 * - onSurface: Color del texto/iconos sobre el color de la superficie.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700), // Amarillo oro
    secondary = Color(0xFF6C6868), // Gris medio
    background = Color.Black, // Fondo negro
    surface = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

/**
 * Define el esquema de colores para el tema claro.
 * - primary: Color principal.
 * - secondary: Color secundario.
 * - background: Color de fondo general.
 * - surface: Color de superficies.
 * - onPrimary: Color del texto/iconos sobre el color primario.
 * - onSecondary: Color del texto/iconos sobre el color secundario.
 * - onBackground: Color del texto/iconos sobre el color de fondo.
 * - onSurface: Color del texto/iconos sobre el color de la superficie.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFD700), // Amarillo oro
    secondary = Color(0xFF6C6868), // Gris medio
    background = Color.White, // Fondo blanco
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
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
        // TODO: Añadir tipografía y formas personalizadas.
    )
}