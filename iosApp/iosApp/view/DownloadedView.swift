import SwiftUI
import shared

/**
 * Vista para la pestaña "Descargas".
 * Muestra una lista de los episodios que el usuario ha descargado para escuchar offline.
 */
struct DownloadedView: View {

    @EnvironmentObject var viewModel: DownloadedViewModel
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.downloadedEpisodios.isEmpty {
                    Text("No tienes episodios descargados.")
                        .foregroundColor(.secondary)
                } else {
                    List {
                        ForEach(viewModel.downloadedEpisodios, id: \.id) { episodio in
                            EpisodioListItemView(
                                episodio: episodio,
                                isLoading: audioManager.isBuffering(episodeId: episodio.id),
                                isDownloaded: true, // Todos en esta lista están descargados
                                onPlay: { audioManager.selectEpisode(episodio) },
                                onAddToQueue: { queueViewModel.addEpisodio(episodio: episodio) },
                                onDeleteDownload: { viewModel.removeDownload(episodio: episodio) }
                            )
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Descargas")
        }
    }
}