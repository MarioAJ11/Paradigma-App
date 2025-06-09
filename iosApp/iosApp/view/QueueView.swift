import SwiftUI
import shared

/**
 * Vista para la pestaña "Cola de Reproducción".
 * Muestra la lista de episodios que el usuario ha añadido para escuchar en orden.
 */
struct QueueView: View {

    @EnvironmentObject var viewModel: QueueViewModel
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.episodiosEnCola.isEmpty {
                    Text("Tu cola de reproducción está vacía.")
                        .foregroundColor(.secondary)
                } else {
                    List {
                        ForEach(viewModel.episodiosEnCola, id: \.id) { episodio in
                            EpisodioListItemView(
                                episodio: episodio,
                                isLoading: audioManager.isBuffering(episodeId: episodio.id),
                                isDownloaded: downloadedViewModel.isDownloaded(episodio: episodio),
                                onPlay: { audioManager.selectEpisode(episodio) },
                                onRemoveFromQueue: { viewModel.removeEpisodio(episodio: episodio) }
                            )
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Cola de Reproducción")
        }
    }
}