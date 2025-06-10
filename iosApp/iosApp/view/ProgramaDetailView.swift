import SwiftUI
import shared

/**
 * Vista que muestra los detalles de un programa y una lista paginada de sus episodios.
 * Ensambla la cabecera (`ProgramaInfoHeader`) y la lista de episodios.
 */
struct ProgramaDetailView: View {

    @StateObject private var viewModel: ProgramaDetailViewModel

    // Gestores globales inyectados desde el entorno
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    init(programa: Programa) {
        _viewModel = StateObject(wrappedValue: ProgramaDetailViewModel(programaId: programa.id))
    }

    var body: some View {
        ScrollView {
            LazyVStack {
                if viewModel.isLoading {
                    ProgressView().padding(.top, 100)
                } else if let programa = viewModel.programa {
                    // Cabecera con la información del programa
                    ProgramaInfoHeader(programa: programa)

                    // Lista de episodios
                    ForEach(viewModel.episodios, id: \.id) { episodio in
                        EpisodioListItemView(
                            episodio: episodio,
                            isLoading: audioManager.isBuffering(episodeId: episodio.id),
                            isDownloaded: downloadedViewModel.isDownloaded(episodio: episodio),
                            isInQueue: queueViewModel.isInQueue(episodio: episodio),
                            onPlay: { audioManager.selectEpisode(episodio) },
                            onToggleQueue: {
                                if queueViewModel.isInQueue(episodio: episodio) {
                                    queueViewModel.removeEpisodio(episodio: episodio)
                                } else {
                                    queueViewModel.addEpisodio(episodio: episodio)
                                }
                            },
                            onToggleDownload: {
                                if downloadedViewModel.isDownloaded(episodio: episodio) {
                                    downloadedViewModel.removeDownload(episodio: episodio)
                                } else {
                                    downloadedViewModel.downloadEpisodio(episodio)
                                }
                            }
                        )
                        .padding(.horizontal)
                        .onAppear {
                            // Carga más episodios cuando el usuario llega al final
                            viewModel.loadMoreEpisodiosIfNeeded(currentEpisodio: episodio)
                        }
                    }

                    // Indicador de carga para la paginación
                    if viewModel.isLoadingMore {
                        ProgressView().padding()
                    }

                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage).foregroundColor(.red).padding()
                }
            }
        }
        .onAppear {
            Task { await viewModel.loadInitialData() }
        }
        .navigationTitle(viewModel.programa?.name.unescaped() ?? "Cargando...")
        .navigationBarTitleDisplayMode(.inline)
    }
}