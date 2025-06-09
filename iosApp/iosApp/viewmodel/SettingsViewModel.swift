import Foundation
import SwiftUI


/**
 * ViewModel que gestiona los ajustes de la aplicación.
 * Utiliza @AppStorage para persistir las opciones del usuario de forma sencilla y reactiva.
 * Replica la funcionalidad del SettingsViewModel de Android.
 */
@MainActor
class SettingsViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas (vinculadas a UserDefaults)

    /**
     * Preferencia para iniciar la app con el stream de radio activo.
     * El valor se guarda automáticamente en UserDefaults con la clave "is_stream_active_on_launch".
     */
    @AppStorage("is_stream_active_on_launch") var isStreamActiveOnLaunch: Bool = true

    /**
     * Preferencia del tema de la aplicación.
     * El valor (raw String) se guarda automáticamente en UserDefaults con la clave "theme_preference".
     */
    @AppStorage("theme_preference") var themePreference: ThemePreference = .system


    // MARK: - Lógica de Negocio

    /**
     * Alterna el estado de la preferencia de inicio con stream.
     * Aunque la vista podría modificar `isStreamActiveOnLaunch` directamente,
     * tener una función en el ViewModel centraliza la lógica de negocio.
     */
    func toggleStreamActive() {
        isStreamActiveOnLaunch.toggle()
    }

    /**
     * Establece la preferencia de tema de la aplicación.
     * - Parameter preference: La nueva preferencia de tema a establecer (sistema, claro u oscuro).
     */
    func setThemePreference(_ preference: ThemePreference) {
        self.themePreference = preference
    }
}