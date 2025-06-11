import SwiftUI

/**
 * Vista que presenta la pantalla de Ajustes de la aplicación.
 * Permite al usuario configurar preferencias y consultar una guía de uso.
 */
struct SettingsView: View {

    @StateObject private var viewModel = SettingsViewModel()
    @Environment(\.openURL) var openURL

    var body: some View {
        NavigationView {
            Form {
                // --- Sección de Preferencias ---
                Section(header: Text("Preferencias Generales")) {
                    Toggle("Abrir con Radio en Directo", isOn: $viewModel.isStreamActiveOnLaunch)
                }

                // --- Sección de Apariencia ---
                Section(header: Text("Apariencia")) {
                    Picker("Tema de la Aplicación", selection: $viewModel.themePreference) {
                        ForEach(ThemePreference.allCases) { theme in
                            Text(theme.rawValue).tag(theme)
                        }
                    }
                }

                // --- NUEVO: Guía de Usuario ---
                Section(header: Text("Ayuda y Funcionalidades")) {
                    HelpItemView(icon: "hand.tap.fill", title: "Ver Detalles del Episodio", description: "Mantén pulsado cualquier episodio en una lista para ver su pantalla de detalles completa.")
                    HelpItemView(icon: "arrow.up.left.and.arrow.down.right.circle.fill", title: "Reproductor Ampliado", description: "Pulsa sobre la información del episodio en el reproductor inferior para abrir la vista a pantalla completa.")
                }

                Section(header: Text("Controles del Reproductor")) {
                    HelpItemView(icon: "play.circle.fill", title: "Reproducir / Pausar", description: "El botón central grande inicia o detiene la reproducción.")
                    HelpItemView(icon: "speaker.wave.2.fill", title: "Radio en Directo", description: "El botón de la antena activa o desactiva la radio en directo.")
                }

                Section(header: Text("Menú de Navegación")) {
                    HelpItemView(icon: "house.fill", title: "Inicio", description: "Vuelve a la pantalla principal con la lista de programas.")
                    HelpItemView(icon: "magnifyingglass", title: "Buscar", description: "Encuentra cualquier episodio por título o descripción.")
                    HelpItemView(icon: "hourglass", title: "Continuar", description: "Episodios que has empezado a escuchar pero no has terminado.")
                    HelpItemView(icon: "arrow.down.circle.fill", title: "Descargas", description: "Accede a los episodios guardados para escucharlos sin conexión.")
                    HelpItemView(icon: "list.bullet", title: "Cola", description: "Organiza una lista de reproducción personalizada.")
                }

                // --- Sección de Información ---
                Section(header: Text("Más Información")) {
                                    Button("Visitar web de Paradigma Media") {
                                        // Obtiene la URL de la web desde la configuración remota.
                                        let urlString = AppServices.shared.getConfig().mainWebsiteUrl
                                        if let url = URL(string: urlString) {
                                            openURL(url)
                                        }
                                    }
                                }
                            }
                            .navigationTitle("Ajustes")
                        }
                    }
                }