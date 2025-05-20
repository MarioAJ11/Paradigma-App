package com.example.paradigmaapp.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography // Importa Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle // Importa TextStyle
import androidx.compose.ui.text.font.FontFamily // Importa FontFamily
import androidx.compose.ui.text.font.FontWeight // Importa FontWeight
import androidx.compose.ui.unit.sp // Importa sp para tamaños de fuente

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
 * Define la tipografía para el tema Material Design 3.
 * Aquí puedes personalizar diferentes estilos de texto (headline, title, body, label)
 * usando TextStyle. Por defecto, se usa FontFamily.Default.
 * Cada estilo de tipografía corresponde a una clase de Material Design.
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle( // Usado en VolumeControl
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle( // Usado en VolumeControl
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle( // Usado en AudioPlayer y PodcastListItem
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Composable que aplica el tema Material Design 3 a la aplicación.
 * Selecciona automáticamente el tema oscuro o claro según la configuración del sistema,
 * a menos que se especifique lo contrario.
 *
 * @param darkTheme Booleano que indica si se debe usar el tema oscuro. Por defecto, usa la configuración del sistema.
 * @param content El contenido Composable al que se aplicará el tema.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Determina si el sistema está en tema oscuro.
    content: @Composable () -> Unit // El bloque de Composables a tematizar.
) {
    // Selecciona el esquema de colores basado en si darkTheme es true.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Aplica el tema MaterialTheme con el esquema de colores y tipografía seleccionados.
    MaterialTheme(
        colorScheme = colorScheme, // Esquema de colores a aplicar.
        typography = AppTypography, // <--- Aquí se añade la tipografía personalizada.
        content = content // Contenido al que se aplica el tema.
        // TODO: Añadir formas personalizadas (ShapeScheme) si es necesario.
    )
}