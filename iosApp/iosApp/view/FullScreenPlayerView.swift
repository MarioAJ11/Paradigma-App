import SwiftUI
import shared

/**
 * Vista del reproductor a pantalla completa.
 * Muestra una carátula grande, título, y controles de reproducción extendidos.
 */
struct FullScreenPlayerView: View {
    @EnvironmentObject var audioManager: AudioManager
    @Environment(\.dismiss) var dismiss

    var body: some View {
        // Solo muestra contenido si hay un episodio.
        // Si no, muestra un estado vacío (aunque no debería poder abrirse sin episodio).
        if let episodio = audioManager.currentEpisode {
            VStack(spacing: 0) {
                // Botón para cerrar la vista
                HStack {
                    Spacer()
                    Button(action: {
                        audioManager.toggleFullScreenPlayer()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.largeTitle)
                            .foregroundColor(.gray.opacity(0.8))
                    }
                }
                .padding()

                Spacer()

                // Carátula del episodio
                AsyncImage(url: URL(string: episodio.imageUrl ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: { Color.surfaceVariant }
                .frame(maxWidth: 350, maxHeight: 350)
                .aspectRatio(1, contentMode: .fit)
                .cornerRadius(16)
                .shadow(radius: 10)
                .padding(.horizontal, 32)

                Spacer()

                // Título y programa
                VStack(spacing: 8) {
                    Text(episodio.title.unescaped())
                        .font(.title2).fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    Text(episodio.embedded?.terms?.first?.first?.name.unescaped() ?? "Paradigma Media")
                        .font(.body).foregroundColor(.amarilloPrincipal)
                }
                .padding()

                // Barra de progreso y tiempos
                VStack(spacing: 4) {
                    Slider(value: Binding(
                        get: { audioManager.progress },
                        set: { newValue in audioManager.seek(to: newValue) }
                    )).tint(.amarilloPrincipal)

                    HStack {
                        Text(formatTime(audioManager.progress * audioManager.duration))
                        Spacer()
                        Text(formatTime(audioManager.duration))
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }
                .padding(.horizontal)

                // Controles de reproducción
                HStack(spacing: 20) {
                    Button(action: { audioManager.rewind() }) {
                        Image(systemName: "gobackward.10").font(.largeTitle)
                    }

                    Button(action: { audioManager.togglePlayPause() }) {
                        Image(systemName: audioManager.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                            .font(.system(size: 72))
                            .foregroundColor(.amarilloPrincipal)
                    }

                    Button(action: { audioManager.skipForward() }) {
                        Image(systemName: "goforward.30").font(.largeTitle)
                    }
                }
                .foregroundColor(.onBackground)
                .padding()

                Spacer()
            }
            .padding()
            .background(Color.background.ignoresSafeArea())
        } else {
            // Estado por si se abre sin episodio
            VStack {
                Text("No hay ningún episodio seleccionado.")
                Button("Cerrar") { audioManager.toggleFullScreenPlayer() }
            }
        }
    }

    // Función para formatear el tiempo de segundos a "mm:ss"
    private func formatTime(_ seconds: Double) -> String {
        guard !seconds.isNaN && !seconds.isInfinite else { return "00:00" }
        let totalSeconds = Int(seconds)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}