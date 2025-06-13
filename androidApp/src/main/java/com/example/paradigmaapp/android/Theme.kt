package com.example.paradigmaapp.android

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

// Definiciones de colores
val BlancoPuro = Color(0xFFFFFFFF)
val NegroPuro = Color(0xFF000000)
val AmarilloPrincipal = Color(0xFFF4CF31)
val AmarilloClaroDecorativo = Color(0xFFFFF7DC)
val AmarilloOscuroContraste = Color(0xFFC7A200)
val GrisMuyClaro = Color(0xFFF5F5F5)
val GrisClaro = Color(0xFFEEEEEE)
val GrisMedioClaro = Color(0xFFE0E0E0)
val GrisMedio = Color(0xFF909090)
val GrisOscuro = Color(0xFF6C6B6B)
val GrisTextoOriginal = Color(0xFF616161)
val GrisMuyOscuro = Color(0xFF424242)
val GrisCasiNegro = Color(0xFF212121)

// Esquema de colores para el Tema Oscuro
private val DarkColorScheme = darkColorScheme(
    primary = AmarilloPrincipal,
    onPrimary = NegroPuro,
    primaryContainer = AmarilloOscuroContraste,
    onPrimaryContainer = AmarilloClaroDecorativo,
    secondary = GrisTextoOriginal,
    onSecondary = BlancoPuro,
    secondaryContainer = GrisMuyOscuro,
    onSecondaryContainer = GrisClaro,
    tertiary = AmarilloClaroDecorativo,
    onTertiary = NegroPuro,
    tertiaryContainer = Color(0xFF4A442A),
    onTertiaryContainer = AmarilloPrincipal,
    background = GrisCasiNegro,
    onBackground = BlancoPuro,
    surface = GrisCasiNegro,
    onSurface = BlancoPuro,
    surfaceVariant = GrisMuyOscuro,
    onSurfaceVariant = GrisMedio,
    surfaceContainerHighest = GrisTextoOriginal,
    surfaceContainerHigh = GrisMuyOscuro,
    surfaceContainer = GrisCasiNegro,
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainerLowest = NegroPuro,
    outline = GrisOscuro,
    outlineVariant = GrisTextoOriginal,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Esquema de colores para el Tema Claro
private val LightColorScheme = lightColorScheme(
    primary = AmarilloPrincipal,
    onPrimary = NegroPuro,
    primaryContainer = AmarilloClaroDecorativo,
    onPrimaryContainer = Color(0xFF5D4200),
    secondary = GrisTextoOriginal,
    onSecondary = BlancoPuro,
    secondaryContainer = GrisClaro,
    onSecondaryContainer = NegroPuro,
    tertiary = AmarilloOscuroContraste,
    onTertiary = BlancoPuro,
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF5D4200),
    background = GrisMuyClaro,
    onBackground = NegroPuro,
    surface = BlancoPuro,
    onSurface = NegroPuro,
    surfaceVariant = GrisClaro,
    onSurfaceVariant = GrisTextoOriginal,
    surfaceContainerHighest = BlancoPuro,
    surfaceContainerHigh = GrisMuyClaro,
    surfaceContainer = GrisClaro,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = GrisMedio,
    outlineVariant = GrisMedioClaro,
    error = Color(0xFFBA1A1A),
    onError = BlancoPuro,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// Definición de la Tipografía de la aplicación
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

/**
 * Composable central que aplica el tema de Material Design 3 a toda la aplicación.
 * Este componente es clave para el cambio de tema dinámico, ya que reacciona a los cambios de estado
 * y redibuja toda la UI anidada con el esquema de colores y tipografía correctos.
 *
 * @param manualDarkThemeSetting La preferencia de tema explícita del usuario.
 * - `true`: Forzar tema oscuro.
 * - `false`: Forzar tema claro.
 * - `null`: Usar la configuración de tema del sistema operativo.
 * @param content El contenido Composable de la aplicación al que se le aplicará este tema.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun Theme(
    manualDarkThemeSetting: Boolean?,
    content: @Composable () -> Unit
) {
    // Esta estructura 'when' determina qué tema usar basándose en la preferencia del usuario.
    // Si la preferencia es nula, recurre a isSystemInDarkTheme() para seguir al sistema.
    val useDarkTheme = when (manualDarkThemeSetting) {
        true -> true
        false -> false
        null -> isSystemInDarkTheme()
    }

    // Se elige el ColorScheme completo (Dark o Light) según la decisión anterior.
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    // SideEffect se usa para ejecutar código que no es de Compose (modificar la Window de Android)
    // cada vez que el composable se recompone.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Ajusta el color de la barra de estado y el color de sus iconos (claros u oscuros).
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme

            // Ajusta el color de la barra de navegación y el color de sus iconos.
            window.navigationBarColor = colorScheme.surfaceContainerLowest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    // MaterialTheme es el componente que propaga los colores y la tipografía
    // a todos los Composables hijos. Al cambiar `colorScheme`, toda la app se actualiza.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}