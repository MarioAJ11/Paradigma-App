import SwiftUI
import shared

/**
 * La vista raíz que contiene la estructura principal de la aplicación.
 *
 * Utiliza un ZStack para superponer el reproductor de audio (`AudioPlayerView`)
 * y el reproductor a pantalla completa (`FullScreenPlayerView`) sobre la
 * navegación principal por pestañas (`TabView`).
 */
struct ContentView: View {

    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel
    @EnvironmentObject var onGoingViewModel: OnGoingViewModel

    /// Define las pestañas disponibles en la barra de navegación inferior.
    enum Tab {
        case inicio, buscar, continuar, descargas, cola, ajustes
    }

    @State private var selection: Tab = .inicio

    var body: some View {
        ZStack(alignment: .bottom) {
            // Navegación principal por pestañas
            TabView(selection: $selection) {
                HomeScreen()
                    .tabItem { Label("Inicio", systemImage: "house.fill") }.tag(Tab.inicio)
                SearchView()
                    .tabItem { Label("Buscar", systemImage: "magnifyingglass") }.tag(Tab.buscar)
                OnGoingView()
                    .tabItem { Label("Continuar", systemImage: "hourglass") }.tag(Tab.continuar)
                DownloadedView()
                    .tabItem { Label("Descargas", systemImage: "arrow.down.circle.fill") }.tag(Tab.descargas)
                QueueView()
                    .tabItem { Label("Cola", systemImage: "list.bullet") }.tag(Tab.cola)
                SettingsView()
                    .tabItem { Label("Ajustes", systemImage: "gear") }.tag(Tab.ajustes)
            }
            .tint(.amarilloPrincipal)

            // Mini-reproductor de audio en la parte inferior
            if audioManager.content != nil && !audioManager.isFullScreenPlayerVisible {
                AudioPlayerView()
                    .padding(.bottom, 48)
                    .ignoresSafeArea(.keyboard)
            }

            // Se mostrará cuando `isFullScreenPlayerVisible` sea true.
            .fullScreenCover(isPresented: $audioManager.isFullScreenPlayerVisible) {
                FullScreenPlayerView()
            }
        }
    }
}