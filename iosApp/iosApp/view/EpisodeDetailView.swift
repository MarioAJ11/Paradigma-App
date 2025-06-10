import SwiftUI
import shared

/**
 * Vista que muestra los detalles completos de un episodio específico.
 * Proporciona información como título, imagen, descripción, fecha, duración,
 * y programas asociados. Ofrece acciones como reproducir, añadir/quitar de cola
 * y descargar/eliminar descarga.
 */
struct EpisodeDetailView: View {

    @StateObject private var viewModel: EpisodeDetailViewModel

    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    init(episodioId: Int32) {
        _viewModel = StateObject(wrappedValue: EpisodeDetailViewModel(episodioId: Int(episodioId)))
    }

    var body: some View {
        ScrollView {
            if viewModel.isLoading {
                ProgressView().padding(.top, 100)
            } else if let episodio = viewModel.episodio {
                mainContent(for: episodio)
            } else if let errorMessage = viewModel.errorMessage {
                Text(errorMessage).foregroundColor(.red).padding()
            }
        }
        .onAppear {
            Task { await viewModel.loadDetails() }
        }
        .navigationTitle(viewModel.episodio?.title.unescaped() ?? "")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func mainContent(for episodio: Episodio) -> some View {
        VStack(spacing: 24) {
            AsyncImage(url: URL(string: episodio.imageUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: { Color.surfaceVariant }
            .aspectRatio(16/9, contentMode: .fit)
            .clipped()

            VStack(spacing: 16) {
                Text(episodio.title.unescaped())
                    .font(.headlineSmall).fontWeight(.bold)
                    .multilineTextAlignment(.center)

                // Metadatos (Fecha, Duración, Programa)
                HStack(spacing: 16) {
                    MetaDataItem(icon: "calendar", text: episodio.date.formattedDate())
                    if episodio.duration.isNotBlank() && episodio.duration != "--:--" {
                        MetaDataItem(icon: "timer", text: episodio.duration)
                    }
                }

                if let programaName = viewModel.programasAsociados.first?.name {
                    Text("Programa: \(programaName.unescaped())")
                        .font(.bodyMedium).foregroundColor(.onSurfaceVariant)
                }

                ActionButtons(episodio: episodio)
            }
            .padding(.horizontal)

            // Descripción
            if let description = (episodio.content ?? episodio.excerpt)?.extractMeaningfulDescription(), !description.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Descripción").font(.titleMedium).fontWeight(.semibold)
                    Text(description)
                        .font(.bodyLarge).foregroundColor(.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
            }
        }
    }

    private struct MetaDataItem: View {
        let icon: String
        let text: String
        var body: some View {
            HStack {
                Image(systemName: icon).font(.bodySmall)
                Text(text).font(.bodySmall)
            }.foregroundColor(.onSurfaceVariant)
        }
    }

    private struct ActionButtons: View {
        let episodio: Episodio
        @EnvironmentObject var audioManager: AudioManager
        @EnvironmentObject var queueViewModel: QueueViewModel
        @EnvironmentObject var downloadedViewModel: DownloadedViewModel

        var body: some View {
            HStack(spacing: 20) {
                // Botón Play
                Button(action: { audioManager.selectEpisode(episodio) }) {
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 44))
                        .foregroundColor(.amarilloPrincipal)
                }

                // Botón Cola
                Button(action: {
                    if queueViewModel.isInQueue(episodio: episodio) {
                        queueViewModel.removeEpisodio(episodio: episodio)
                    } else {
                        queueViewModel.addEpisodio(episodio: episodio)
                    }
                }) {
                    VStack {
                        Image(systemName: queueViewModel.isInQueue(episodio: episodio) ? "text.badge.minus" : "text.badge.plus")
                            .font(.title2)
                        Text("Cola").font(.caption)
                    }
                }

                // Botón Descarga
                Button(action: {
                    if downloadedViewModel.isDownloaded(episodio: episodio) {
                        downloadedViewModel.removeDownload(episodio: episodio)
                    } else {
                        downloadedViewModel.downloadEpisodio(episodio)
                    }
                }) {
                    VStack {
                        Image(systemName: downloadedViewModel.isDownloaded(episodio: episodio) ? "trash.circle" : "arrow.down.circle")
                            .font(.title2)
                        Text(downloadedViewModel.isDownloaded(episodio: episodio) ? "Borrar" : "Bajar")
                            .font(.caption)
                    }
                }
            }
            .buttonStyle(.plain)
            .foregroundColor(.onBackground)
        }
    }
}