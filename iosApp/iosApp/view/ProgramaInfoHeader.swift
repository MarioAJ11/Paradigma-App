import SwiftUI
import shared

/**
 * Subvista para la cabecera con la imagen, título y descripción del programa.
 */
struct ProgramaInfoHeader: View {
    let programa: Programa

    var body: some View {
        VStack(spacing: 16) {
            AsyncImage(url: URL(string: programa.imageUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: { Color.surfaceVariant }
            .frame(width: 180, height: 180)
            .cornerRadius(12)
            .shadow(radius: 5)

            Text(programa.name.unescaped())
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            if let description = programa.description_ {
                Text(description.extractMeaningfulDescription())
                    .font(.body)
                    .foregroundColor(.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 24)
    }
}