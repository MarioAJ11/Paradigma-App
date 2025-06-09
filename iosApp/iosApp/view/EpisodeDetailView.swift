import SwiftUI
import shared

/**
 * Vista que muestra los detalles completos de un episodio específico.
 * Proporciona información como título, imagen, descripción, fecha, duración,
 * y programas asociados. Ofrece acciones como reproducir, añadir/quitar de cola
 * y descargar/eliminar descarga.
 */
struct EpisodeDetailView: View {

    // MARK: - Propiedades

    @StateObject private var viewModel: EpisodeDetailViewModel

    // Gestores globales para las acciones del usuario.
    @EnvironmentObject var audioManager: AudioManager
    @EnvironmentObject var queueViewModel: QueueViewModel
    @EnvironmentObject var downloadedViewModel: DownloadedViewModel

    // MARK: - Inicializador

    init(episodioId: Int32) {
        _viewModel = StateObject(wrappedValue: EpisodeDetailViewModel(episodioId: Int(episodioId)))
    }

    // MARK: - Cuerpo de la Vista

    var body: some View {
        ScrollView {
            // Muestra un indicador de carga mientras se obtienen los datos.
            if viewModel.isLoading {
                ProgressView().padding(.top, 100)
            } else if let episodio = viewModel.episodio {
                // Muestra el contenido principal una vez cargado el episodio.
                mainContent(for: episodio)
            } else if let errorMessage = viewModel.errorMessage {
                // Muestra un mensaje de error si algo falló.
                Text(errorMessage).foregroundColor(.red).padding()
            }
        }
        .onAppear {
            Task { await viewModel.loadDetails() }
        }
        .navigationTitle(viewModel.episodio?.title.rendered.unescaped() ?? "")
        .navigationBarTitleDisplayMode(.inline)
    }

    /**
     * Construye la vista principal con todos los detalles del episodio.
     * - Parameter episodio: El objeto `Episodio` a mostrar.
     * - Returns: Una vista de SwiftUI con el contenido.
     */
    @ViewBuilder
    private func mainContent(for episodio: Episodio) -> some View {
        VStack(spacing: 24) {
            // Imagen del episodio
            AsyncImage(url: URL(string: episodio.imageUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: { Color.surfaceVariant }
            .aspectRatio(16/9, contentMode: .fit)
            .clipped()

            // Título
            Text(episodio.title.rendered.unescaped())
                .font(.headline)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // Metadatos (Fecha, Duración)
            HStack(spacing: 24) {
                MetaDataItem(icon: "calendar", text: episodio.date.formattedDate())
                if episodio.duration.isNotBlank() && episodio.duration != "--:--" {
                    MetaDataItem(icon: "timer", text: episodio.duration)
                }
            }

            // Botones de Acción
            ActionButtons(
                episodio: episodio,
                audioManager: audioManager,
                queueViewModel: queueViewModel,
                downloadedViewModel: downloadedViewModel
            )

            // Descripción del episodio
            VStack(alignment: .leading, spacing: 8) {
                if let description = episodio.content?.rendered.extractMeaningfulDescription(), !description.isEmpty {
                    Text("Descripción")
                        .font(.title3)
                        .fontWeight(.semibold)

                    Text(description)
                        .font(.body)
                        .foregroundColor(.onSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
        }
    }
}

// MARK: - Subvistas Privadas

/**
 * Subvista para mostrar un metadato con un icono y texto.
 */
private struct MetaDataItem: View {
    let icon: String
    let text: String

    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.subheadline)
            Text(text)
                .font(.subheadline)
        }
        .foregroundColor(.onSurfaceVariant)
    }
}

/**
 * Subvista para los botones de acción principales (Reproducir, Cola, Descargar).
 */
private struct ActionButtons: View {
    let episodio: Episodio
    @ObservedObject var audioManager: AudioManager
    @ObservedObject var queueViewModel: QueueViewModel
    @ObservedObject var downloadedViewModel: DownloadedViewModel

    var body: some View {
        HStack(spacing: 24) {
            // Botón de Reproducir/Pausar
            Button(action: { audioManager.selectEpisode(episodio) }) {
                Label("Reproducir", systemImage: audioManager.isPlaying && audioManager.currentEpisode?.id == episodio.id ? "pause.fill" : "play.fill")
            }
            .buttonStyle(.borderedProminent)
            .tint(.primary)

            // Botón de Añadir/Quitar de la Cola
            Button(action: {
                if queueViewModel.isInQueue(episodio: episodio) {
                    queueViewModel.removeEpisodio(episodio: episodio)
                } else {
                    queueViewModel.addEpisodio(episodio: episodio)
                }
            }) {
                Image(systemName: queueViewModel.isInQueue(episodio: episodio) ? "text.badge.minus" : "text.badge.plus")
            }
            .buttonStyle(.bordered)

            // Botón de Descargar/Eliminar Descarga
            Button(action: {
                if downloadedViewModel.isDownloaded(episodio: episodio) {
                    downloadedViewModel.removeDownload(episodio: episodio)
                } else {
                    downloadedViewModel.addDownload(episodio: episodio)
                }
            }) {
                Image(systemName: downloadedViewModel.isDownloaded(episodio: episodio) ? "trash.circle" : "arrow.down.circle")
            }
            .buttonStyle(.bordered)
        }
        .font(.title2)
    }
}