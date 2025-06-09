import SwiftUI
import shared

/**
 * Vista que representa un único ítem de Programa en una cuadrícula.
 * Muestra una imagen y el título del programa en una tarjeta clicable.
 */
struct ProgramaListItemView: View {
    let programa: Programa

    var body: some View {
        VStack(alignment: .center, spacing: 0) {
            // AsyncImage carga la imagen desde una URL de forma asíncrona.
            AsyncImage(url: URL(string: programa.imageUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                // Mientras carga la imagen, muestra un indicador.
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.surfaceVariant)
            }
            .frame(height: 150)
            .clipped()

            // Contenedor para el título, con una altura mínima para alinear las tarjetas.
            VStack {
                Text(programa.name.unescaped()) // Usamos la extensión para decodificar HTML
                    .font(.titleSmall)
                    .fontWeight(.semibold)
                    .foregroundColor(.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                Spacer()
            }
            .padding(8)
            .frame(height: 70)
            .frame(maxWidth: .infinity)
        }
        .background(Color.surfaceVariant)
        .cornerRadius(12)
        .shadow(radius: 2, y: 1)
    }
}