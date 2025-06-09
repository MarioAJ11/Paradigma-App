import SwiftUI
import shared

/**
 * Vista para la pestaña "Continuar Escuchando".
 * Muestra una lista de los episodios que el usuario ha empezado a escuchar pero no ha terminado.
 */
struct OnGoingView: View {

    @EnvironmentObject var viewModel: OnGoingViewModel
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.onGoingEpisodios.isEmpty {
                    Text("No tienes episodios en progreso.")
                        .foregroundColor(.secondary)
                } else {
                    List {
                        ForEach(viewModel.onGoingEpisodios, id: \.id) { episodio in
                            EpisodioListItemView(
                                episodio: episodio,
                                isLoading: audioManager.isBuffering(episodeId: episodio.id),
                                isDownloaded: downloadedViewModel.isDownloaded(episodio: episodio),
                                onPlay: { audioManager.selectEpisode(episodio) },
                                onAddToQueue: { queueViewModel.addEpisodio(episodio: episodio) }
                            )
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Continuar")
            .onAppear {
                Task { await viewModel.loadOnGoingEpisodios() }
            }
        }
    }
}