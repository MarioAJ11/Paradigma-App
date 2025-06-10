import Foundation
import SwiftUI

/**
 * ViewModel que gestiona los ajustes de la aplicación.
 * Utiliza @AppStorage para persistir las opciones del usuario de forma sencilla y reactiva.
 */
@MainActor
class SettingsViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas (vinculadas a UserDefaults)

    /**
     * NUEVO: Estado que controla si el usuario ya ha completado la pantalla de bienvenida.
     * El valor se guarda automáticamente en UserDefaults con la clave "onboarding_complete".
     */
    @AppStorage("onboarding_complete") var hasCompletedOnboarding: Bool = false

    /**
     * Preferencia para iniciar la app con el stream de radio activo.
     */
    @AppStorage("is_stream_active_on_launch") var isStreamActiveOnLaunch: Bool = true

    /**
     * Preferencia del tema de la aplicación.
     */
    @AppStorage("theme_preference") var themePreference: ThemePreference = .system


    // MARK: - Lógica de Negocio

    /**
     * NUEVO: Marca el proceso de onboarding como completado.
     * Esta función será llamada por la vista de Onboarding.
     */
    func completeOnboarding() {
        hasCompletedOnboarding = true
    }

    /**
     * Alterna el estado de la preferencia de inicio con stream.
     */
    func toggleStreamActive() {
        isStreamActiveOnLaunch.toggle()
    }

    /**
     * Establece la preferencia de tema de la aplicación.
     */
    func setThemePreference(_ preference: ThemePreference) {
        self.themePreference = preference
    }
}