package com.example.paradigmaapp.android

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// --- Paleta de Colores Base Expandida ---

// Colores Fundamentales
val BlancoPuro = Color(0xFFFFFFFF)
val NegroPuro = Color(0xFF000000) // Tu NegroFondo

// Tonos de Amarillo (Color de Acento Principal de la Marca)
val AmarilloPrincipal = Color(0xFFF4CF31) // Tu AmarilloRadio
val AmarilloClaroDecorativo = Color(0xFFFFF7DC) // Tu "Otros", ideal para fondos sutiles de contenedores primarios en tema claro o acentos terciarios.
val AmarilloOscuroContraste = Color(0xFFC7A200) // Para contenedores primarios en tema oscuro o acentos más fuertes.

// Escala de Grises Detallada
val GrisMuyClaro = Color(0xFFF5F5F5)  // casi blanco, para fondos en tema claro
val GrisClaro = Color(0xFFEEEEEE)    // para variantes de superficie en tema claro
val GrisMedioClaro = Color(0xFFE0E0E0) // para outlines sutiles en tema claro
val GrisMedio = Color(0xFFBDBDBD)      // para outlines más definidos en tema claro o texto secundario
val GrisMedioOscuro = Color(0xFF9E9E9E)  // para texto secundario o variantes de superficie en tema oscuro
val GrisOscuro = Color(0xFF757575)      // para outlines en tema oscuro
val GrisTextoOriginal = Color(0xFF616161) // Tu GrisTextoOscuro, puede ser un color secundario.
val GrisMuyOscuro = Color(0xFF424242)    // para superficies sutilmente diferenciadas en tema oscuro
val GrisCasiNegro = Color(0xFF212121)    // para fondos o superficies principales en tema oscuro, alternativa al NegroPuro


/**
 * Esquema de colores para el Tema Oscuro.
 * Prioriza el AmarilloPrincipal sobre fondos oscuros, usando blancos y grises claros para el texto.
 */
private val DarkColorScheme = darkColorScheme(
    primary = AmarilloPrincipal,              // Color principal para elementos interactivos clave.
    onPrimary = NegroPuro,                    // Texto/iconos sobre 'primary'. Negro ofrece buen contraste sobre amarillo.

    primaryContainer = AmarilloOscuroContraste, // Un contenedor con el color primario, pero más oscuro.
    onPrimaryContainer = AmarilloClaroDecorativo, // Texto/iconos sobre 'primaryContainer'.

    secondary = GrisTextoOriginal,            // Color secundario para elementos menos prominentes.
    onSecondary = BlancoPuro,                 // Texto/iconos sobre 'secondary'.

    secondaryContainer = GrisMuyOscuro,       // Contenedor para elementos secundarios, más oscuro que el 'secondary'.
    onSecondaryContainer = GrisClaro,         // Texto/iconos sobre 'secondaryContainer'.

    tertiary = AmarilloClaroDecorativo,       // Color terciario para acentos adicionales o roles decorativos.
    onTertiary = NegroPuro,                   // Texto/iconos sobre 'tertiary'.

    tertiaryContainer = Color(0xFF4A442A),    // Un contenedor terciario oscuro que complementa el amarillo claro.
    onTertiaryContainer = AmarilloPrincipal,  // Texto/iconos sobre 'tertiaryContainer'.

    background = NegroPuro,                   // Fondo principal de la aplicación.
    onBackground = BlancoPuro,                // Color del texto y los iconos principales sobre 'background'.

    surface = GrisCasiNegro,                  // Color de las superficies de componentes como Cards, Sheets, Menus.
    onSurface = BlancoPuro,                   // Color del texto y los iconos sobre 'surface'.

    surfaceVariant = GrisMuyOscuro,           // Una variante de 'surface' con un énfasis diferente (ej. cabeceras de listas).
    onSurfaceVariant = GrisMedio,             // Color del texto y los iconos sobre 'surfaceVariant'.

    surfaceContainerHighest = GrisTextoOriginal, // La superficie con mayor elevación tonal en el tema oscuro.
    surfaceContainerHigh = GrisMuyOscuro,
    surfaceContainer = GrisCasiNegro,
    surfaceContainerLow = Color(0xFF181818),     // Un poco más claro que NegroPuro
    surfaceContainerLowest = NegroPuro,

    outline = GrisOscuro,                     // Bordes decorativos o divisores.
    outlineVariant = GrisTextoOriginal,       // Bordes para énfasis o separación más sutil.

    error = Color(0xFFFFB4AB),                // Color estándar de error en tema oscuro.
    onError = Color(0xFF690005),              // Texto/iconos sobre 'error'.
    errorContainer = Color(0xFF93000A),        // Contenedor para elementos de error.
    onErrorContainer = Color(0xFFFFDAD6)      // Texto/iconos sobre 'errorContainer'.
)

/**
 * Esquema de colores para el Tema Claro.
 * Usa el AmarilloPrincipal como color primario, con fondos claros y texto oscuro.
 */
private val LightColorScheme = lightColorScheme(
    primary = AmarilloPrincipal,              // Color principal.
    onPrimary = NegroPuro,                    // Texto/iconos sobre 'primary'.

    primaryContainer = AmarilloClaroDecorativo, // Contenedor primario, un amarillo muy pálido.
    onPrimaryContainer = Color(0xFF5D4200),    // Texto oscuro sobre el contenedor primario claro.

    secondary = GrisTextoOriginal,            // Color secundario.
    onSecondary = BlancoPuro,                 // Texto/iconos sobre 'secondary'.

    secondaryContainer = GrisClaro,           // Contenedor secundario, un gris claro.
    onSecondaryContainer = NegroPuro,         // Texto/iconos sobre 'secondaryContainer'.

    tertiary = AmarilloOscuroContraste,       // Color terciario.
    onTertiary = BlancoPuro,                  // Texto/iconos sobre 'tertiary'.

    tertiaryContainer = Color(0xFFFFE082),    // Contenedor terciario, un amarillo medio.
    onTertiaryContainer = Color(0xFF5D4200),  // Texto/iconos sobre 'tertiaryContainer'.

    background = GrisMuyClaro,                // Fondo principal, un gris muy claro, casi blanco.
    onBackground = NegroPuro,                 // Texto/iconos sobre 'background'.

    surface = BlancoPuro,                     // Superficies como tarjetas, generalmente blanco puro en tema claro.
    onSurface = NegroPuro,                    // Texto/iconos sobre 'surface'.

    surfaceVariant = GrisClaro,               // Variante de superficie, para elementos como divisores o fondos de campos de texto.
    onSurfaceVariant = GrisTextoOriginal,     // Texto/iconos sobre 'surfaceVariant'.

    surfaceContainerHighest = BlancoPuro,
    surfaceContainerHigh = GrisMuyClaro,
    surfaceContainer = GrisClaro,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF), // Mismo que surface en este caso

    outline = GrisMedio,                      // Bordes.
    outlineVariant = GrisMedioClaro,          // Bordes más sutiles.

    error = Color(0xFFBA1A1A),                // Color estándar de error en tema claro.
    onError = BlancoPuro,                     // Texto/iconos sobre 'error'.
    errorContainer = Color(0xFFFFDAD6),        // Contenedor para elementos de error.
    onErrorContainer = Color(0xFF410002)      // Texto/iconos sobre 'errorContainer'.
)

/**
 * Define la tipografía para el tema Material Design 3.
 * (Tu definición de AppTypography se mantiene igual que en tu última versión)
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
    headlineSmall = TextStyle(
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
    titleMedium = TextStyle(
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
    bodyMedium = TextStyle(
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
 * (Restaurado el nombre a `MyApplicationTheme` y añadido el control de la barra de estado)
 *
 * @param darkTheme Booleano que indica si se debe usar el tema oscuro. Por defecto, usa la configuración del sistema.
 * @param content El contenido Composable al que se aplicará el tema.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Establecer color de la barra de estado y apariencia de iconos
            window.statusBarColor = colorScheme.background.toArgb() // Fondo de la app para la barra de estado
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme // Iconos oscuros en tema claro, claros en tema oscuro

            window.navigationBarColor = colorScheme.surfaceContainerLowest.toArgb() // Un color sutil para la barra de navegación
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}