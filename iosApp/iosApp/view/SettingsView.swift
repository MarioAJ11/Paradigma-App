import SwiftUI

/**
 * Vista que presenta la pantalla de Ajustes de la aplicación.
 * Permite al usuario configurar preferencias como el tema y el comportamiento de inicio.
 * Se comunica con `SettingsViewModel` para leer y escribir estas preferencias.
 */
struct SettingsView: View {

    // Usamos @StateObject para crear y mantener viva una instancia del ViewModel
    // mientras esta vista esté activa.
    @StateObject private var viewModel = SettingsViewModel()

    // Obtenemos una referencia al manejador de URLs para abrir enlaces externos.
    @Environment(\.openURL) var openURL

    var body: some View {
        // NavigationView proporciona la barra de título superior y la capacidad de navegación.
        NavigationView {
            // Form es el contenedor estándar en iOS para pantallas de ajustes.
            // Agrupa visualmente los elementos.
            Form {
                // MARK: - Sección de Preferencias
                Section(header: Text("Preferencias Generales")) {
                    // El Toggle se vincula directamente a la propiedad @AppStorage del ViewModel.
                    // Cualquier cambio en el interruptor actualiza la preferencia guardada.
                    Toggle("Abrir con Radio en Directo", isOn: $viewModel.isStreamActiveOnLaunch)
                }

                // MARK: - Sección de Apariencia
                Section(header: Text("Apariencia")) {
                    // El Picker permite al usuario elegir una opción de una lista.
                    // Se vincula a la propiedad `themePreference` de nuestro ViewModel.
                    Picker("Tema de la Aplicación", selection: $viewModel.themePreference) {
                        // Iteramos sobre todas las opciones de nuestro enum `ThemePreference`.
                        ForEach(ThemePreference.allCases) { theme in
                            Text(theme.rawValue).tag(theme)
                        }
                    }
                    // .pickerStyle(.navigationLink) es un estilo común en iOS para selectores.
                }

                // MARK: - Sección de Información
                Section(header: Text("Más Información")) {
                    // Botón que abre el navegador web del sistema.
                    Button("Visitar web de Paradigma Media") {
                        if let url = URL(string: "https://paradigmamedia.org/") {
                            openURL(url)
                        }
                    }
                }
            }
            .navigationTitle("Ajustes") // Título que aparece en la barra de navegación.
        }
    }
}