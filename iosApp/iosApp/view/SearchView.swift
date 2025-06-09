import SwiftUI
import shared

/**
 * Vista para la pantalla de Búsqueda.
 *
 * Permite al usuario buscar episodios por un término de búsqueda. Muestra los resultados
 * en una lista y gestiona los estados de carga, vacío y error utilizando `SearchViewModel`.
 */
struct SearchView: View {

    // MARK: - Propiedades

    // Crea una instancia del ViewModel para esta vista.
    @StateObject private var viewModel = SearchViewModel()

    // Recibe los gestores globales desde el entorno.
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    // MARK: - Cuerpo de la Vista

    var body: some View {
        NavigationView {
            ZStack {
                // Muestra la lista de resultados.
                List(viewModel.searchResults, id: \.id) { episodio in
                    // Reutilizamos nuestra celda de episodio.
                    EpisodioListItemView(
                        episodio: episodio,
                        isLoading: audioManager.isBuffering(episodeId: episodio.id),
                        isDownloaded: downloadedViewModel.isDownloaded(episodio: episodio),
                        onPlay: { audioManager.selectEpisode(episodio) },
                        onAddToQueue: { queueViewModel.addEpisodio(episodio: episodio) },
                        onRemoveFromQueue: { queueViewModel.removeEpisodio(episodio: episodio) },
                        onDownload: { downloadedViewModel.addDownload(episodio: episodio) },
                        onDeleteDownload: { downloadedViewModel.removeDownload(episodio: episodio) }
                    )
                    // Estilos para una apariencia limpia en la lista.
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                }
                .listStyle(.plain)

                // Superpone un mensaje o un indicador de carga sobre la lista.
                if viewModel.isLoading {
                    ProgressView()
                } else if let infoMessage = viewModel.infoMessage {
                    Text(infoMessage)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                }
            }
            .navigationTitle("Buscar")
            // El modificador .searchable añade la barra de búsqueda nativa de iOS.
            // El texto se vincula directamente a la propiedad del ViewModel,
            // y la lógica de debounce que programamos se encarga del resto.
            .searchable(
                text: $viewModel.searchText,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: "Buscar episodios..."
            )
        }
    }
}