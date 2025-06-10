import SwiftUI
import shared

/**
 * Vista del reproductor de audio global que aparece en la parte inferior de la pantalla.
 * Se actualiza en tiempo real observando los cambios del `AudioManager`.
 */
struct AudioPlayerView: View {
    @EnvironmentObject var audioManager: AudioManager

    var body: some View {
        VStack(spacing: 0) {
            Slider(value: Binding(
                get: { audioManager.progress },
                set: { newValue in audioManager.seek(to: newValue) }
            ))

            HStack(spacing: 12) {
                // Tocar la imagen o el título ahora muestra el reproductor completo.
                Button(action: {
                    audioManager.toggleFullScreenPlayer()
                }) {
                    HStack {
                        AsyncImage(url: URL(string: audioManager.currentEpisode?.imageUrl ?? "")) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: { Color.surfaceVariant }
                        .frame(width: 56, height: 56)
                        .cornerRadius(6)

                        Text(audioManager.currentEpisode?.title.unescaped() ?? "Radio en Directo")
                            .font(.bodyMedium)
                            .lineLimit(2)
                            .foregroundColor(.onBackground)
                    }
                }
                .buttonStyle(.plain)

                Spacer()

                Button(action: { audioManager.togglePlayPause() }) {
                    Image(systemName: audioManager.isPlaying ? "pause.fill" : "play.fill")
                        .font(.title)
                }
                .foregroundColor(.onBackground)
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(.thinMaterial)
        .transition(.move(edge: .bottom).animation(.default))
    }
}