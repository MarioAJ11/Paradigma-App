import SwiftUI

/**
 * Punto de entrada principal de la aplicación de iOS.
 *
 * Responsabilidades:
 * - Inicializar los gestores y ViewModels globales (`SettingsViewModel`, `AudioManager`, etc.).
 * - Inyectar los objetos globales en el entorno de SwiftUI para que estén disponibles en toda la app.
 * - Aplicar el tema (claro/oscuro/sistema) a la vista raíz.
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
			ContentView()
                .preferredColorScheme(getPreferredColorScheme())
                .environmentObject(audioManager)
                .environmentObject(queueViewModel)
                .environmentObject(downloadedViewModel)
                .environmentObject(onGoingViewModel)
		}
	}

    /// Determina el esquema de color a aplicar basándose en la preferencia guardada.
    private func getPreferredColorScheme() -> ColorScheme? {
        switch settingsViewModel.themePreference {
        case .light:
            return .light
        case .dark:
            g_button
            return .dark
        case .system:
            return nil
        }
    }
}