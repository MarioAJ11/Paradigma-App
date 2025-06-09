import Foundation
import SwiftUI

// MARK: - Definición de Colores

// Primero, añadimos una pequeña ayuda para poder crear colores a partir de valores hexadecimales,
// igual que en Android.
extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

/**
 * Extensión de `Color` para definir la paleta de colores personalizada de la aplicación.
 * Esto nos permite usar los colores de forma centralizada y consistente, como `Color.amarilloPrincipal`.
 */
extension Color {
    static let blancoPuro = Color(hex: 0xFFFFFFFF)
    static let negroPuro = Color(hex: 0xFF000000)
    static let amarilloPrincipal = Color(hex: 0xFFF4CF31)
    static let amarilloClaroDecorativo = Color(hex: 0xFFFFF7DC)
    static let amarilloOscuroContraste = Color(hex: 0xFFC7A200)
    static let grisMuyClaro = Color(hex: 0xFFF5F5F5)
    static let grisClaro = Color(hex: 0xFFEEEEEE)
    static let grisMedioClaro = Color(hex: 0xFFE0E0E0)
    static let grisMedio = Color(hex: 0xFF909090)
    static let grisOscuro = Color(hex: 0xFF6C6B6B)
    static let grisTextoOriginal = Color(hex: 0xFF616161)
    static let grisMuyOscuro = Color(hex: 0xFF424242)
    static let grisCasiNegro = Color(hex: 0xFF212121)

    // Colores semánticos de SwiftUI que se adaptan automáticamente a claro/oscuro.
    // Los definimos aquí para tener un único lugar donde consultar todos los colores.
    static let background = Color(uiColor: .systemBackground)
    static let onBackground = Color(uiColor: .label)
    static let surface = Color(uiColor: .systemGray6)
    static let onSurface = Color(uiColor: .label)
    static let surfaceVariant = Color(uiColor: .systemGray5)
    static let onSurfaceVariant = Color(uiColor: .secondaryLabel)
    static let primary = amarilloPrincipal
    static let onPrimary = negroPuro
}


// MARK: - Definición de Tipografía

/**
 * Extensión de `Font` para definir la escala de tipografía de la aplicación.
 * Replica los estilos definidos en `Typography` de Material Design para Android.
 * Nos permite usar fuentes de forma consistente, como `Font.headlineSmall`.
 */
extension Font {
    static let displayLarge: Font = .system(size: 57, weight: .regular)
    static let displayMedium: Font = .system(size: 45, weight: .regular)
    static let displaySmall: Font = .system(size: 36, weight: .regular)

    static let headlineLarge: Font = .system(size: 32, weight: .regular)
    static let headlineMedium: Font = .system(size: 28, weight: .regular)
    static let headlineSmall: Font = .system(size: 24, weight: .regular)

    static let titleLarge: Font = .system(size: 22, weight: .regular)
    static let titleMedium: Font = .system(size: 16, weight: .medium)
    static let titleSmall: Font = .system(size: 14, weight: .medium)

    static let bodyLarge: Font = .system(size: 16, weight: .regular)
    static let bodyMedium: Font = .system(size: 14, weight: .regular)
    static let bodySmall: Font = .system(size: 12, weight: .regular)

    static let labelLarge: Font = .system(size: 14, weight: .medium)
    static let labelMedium: Font = .system(size: 12, weight: .medium)
    static let labelSmall: Font = .system(size: 11, weight: .medium)
}