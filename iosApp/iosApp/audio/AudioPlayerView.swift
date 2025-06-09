import SwiftUI
import shared

/**
 * Vista del reproductor de audio global que aparece en la parte inferior de la pantalla.
 * Se actualiza en tiempo real observando los cambios del `AudioManager`.
 */
struct AudioPlayerView: View {
    // Con @EnvironmentObject, la vista recibe la instancia compartida de AudioManager.
    @EnvironmentObject var audioManager: AudioManager

    var body: some View {
        // Solo muestra la vista si hay un episodio cargado.
        if let episodio = audioManager.currentEpisode {
            VStack(spacing: 0) {
                // Barra de progreso.
                Slider(value: Binding(
                    get: { audioManager.progress },
                    set: { newValue in
                        audioManager.seek(to: newValue)
                    }
                ))

                // Contenido principal del reproductor.
                HStack(spacing: 12) {
                    // Imagen del episodio
                    AsyncImage(url: URL(string: episodio.imageUrl ?? "")) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: { Color.surfaceVariant }
                        .frame(width: 56, height: 56)
                        .cornerRadius(6)

                    // Título del episodio
                    Text(episodio.title.unescaped())
                        .font(.bodyMedium)
                        .lineLimit(2)

                    Spacer()

                    // Botón de Play/Pause
                    Button(action: {
                        audioManager.togglePlayPause()
                    }) {
                        Image(systemName: audioManager.isPlaying ? "pause.fill" : "play.fill")
                            .font(.title)
                            .foregroundColor(.onBackground)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
            }
            .background(.thinMaterial) // Un fondo translúcido que se adapta.
            .transition(.move(edge: .bottom)) // Animación de entrada/salida.
        }
    }
}