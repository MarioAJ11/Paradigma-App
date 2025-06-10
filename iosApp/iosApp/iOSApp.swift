import SwiftUI

/**
 * Punto de entrada principal de la aplicación de iOS.
 *
 * Responsabilidades:
 * - Decidir si mostrar la pantalla de Onboarding o la aplicación principal.
 * - Inicializar y proveer los gestores y ViewModels globales al entorno de SwiftUI.
 */
@main
struct iOSApp: App {

    // MARK: - Gestores Globales y ViewModels

    @StateObject private var settingsViewModel = SettingsViewModel()
    @StateObject private var audioManager = AudioManager.shared
    @StateObject private var queueViewModel = QueueViewModel()
    @StateObject private var downloadedViewModel = DownloadedViewModel()
    @StateObject private var onGoingViewModel = OnGoingViewModel()

    var body: some Scene {
        WindowGroup {
            // Se comprueba el estado `hasCompletedOnboarding` del ViewModel.
            if settingsViewModel.hasCompletedOnboarding {
                // Si ya se completó, muestra la app principal.
                ContentView()
                    .preferredColorScheme(getPreferredColorScheme())
                    .environmentObject(settingsViewModel) // Inyectamos también settings
                    .environmentObject(audioManager)
                    .environmentObject(queueViewModel)
                    .environmentObject(downloadedViewModel)
                    .environmentObject(onGoingViewModel)
            } else {
                // Si no se ha completado, muestra la pantalla de bienvenida.
                OnboardingView()
                    .environmentObject(settingsViewModel) // La vista de onboarding necesita el ViewModel
            }
        }
    }

    /// Determina el esquema de color a aplicar basándose en la preferencia guardada.
    private func getPreferredColorScheme() -> ColorScheme? {
        switch settingsViewModel.themePreference {
        case .light:
            return .light
        case .dark:
            return .dark
        case .system:
            return nil
        }
    }
}