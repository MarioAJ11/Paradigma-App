import SwiftUI
import shared

/**
 * Vista que representa una fila para un único episodio en una lista.
 * Es un componente reutilizable para mostrar la información básica del episodio
 * y proporcionar interacciones como la reproducción y un menú de opciones.
 */
struct EpisodioListItemView: View {
    // MARK: - Propiedades

    let episodio: Episodio
    let isLoading: Bool
    let isDownloaded: Bool

    var onPlay: () -> Void
    var onAddToQueue: (() -> Void)? = nil
    var onRemoveFromQueue: (() -> Void)? = nil
    var onDownload: (() -> Void)? = nil
    var onDeleteDownload: (() -> Void)? = nil

    // MARK: - Cuerpo de la Vista

    var body: some View {
        Button(action: onPlay) {
            HStack(spacing: 12) {
                ZStack {
                    if isLoading {
                        ProgressView()
                    } else {
                        AsyncImage(url: URL(string: episodio.imageUrl ?? "")) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: { Color.surfaceVariant }
                    }
                }
                .frame(width: 64, height: 64)
                .background(Color.surfaceVariant)
                .cornerRadius(8)
                .clipped()

                VStack(alignment: .leading, spacing: 4) {
                    Text(episodio.title.unescaped())
                        .font(.bodyMedium).fontWeight(.medium)
                        .foregroundColor(.onBackground).lineLimit(2)
                    if episodio.duration.isNotBlank() && episodio.duration != "--:--" {
                        Text(episodio.duration)
                            .font(.bodySmall).foregroundColor(.onSurfaceVariant)
                    }
                }
                Spacer()
                Menu {
                    // Opciones de la Cola
                    if let onAddToQueue = onAddToQueue {
                        Button("Añadir a la cola", systemImage: "text.badge.plus", action: onAddToQueue)
                    }
                    if let onRemoveFromQueue = onRemoveFromQueue {
                        Button("Quitar de la cola", systemImage: "text.badge.minus", role: .destructive, action: onRemoveFromQueue)
                    }

                    // Opciones de Descarga
                    if isDownloaded {
                        if let onDeleteDownload = onDeleteDownload {
                            Button("Eliminar descarga", systemImage: "trash", role: .destructive, action: onDeleteDownload)
                        }
                    } else {
                        if let onDownload = onDownload {
                            Button("Descargar episodio", systemImage: "arrow.down.circle", action: onDownload)
                        }
                    }

                } label: {
                    Image(systemName: "ellipsis")
                        .font(.body).foregroundColor(.onSurfaceVariant)
                        .frame(width: 44, height: 44, alignment: .center)
                        .contentShape(Rectangle())
                }.onTapGesture {}
            }
            .padding(8)
        }
        .background(Color.surface)
        .cornerRadius(12)
        .buttonStyle(.plain)
    }
}