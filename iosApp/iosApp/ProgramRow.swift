import SwiftUI
import shared // Importamos nuestro módulo compartido

struct ProgramRow: View {
    // Esta vista recibe un objeto 'Programa' directamente desde Kotlin
    let programa: Programa

    var body: some View {
        VStack(alignment: .center, spacing: 8) {
            // AsyncImage es el equivalente en SwiftUI a Coil en Compose.
            // Carga una imagen desde una URL de forma asíncrona.
            // Es necesario iOS 15 o superior.
            AsyncImage(url: URL(string: programa.imageUrl ?? "")) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                // Imagen que se muestra mientras carga o si hay un error
                Image("logo_foreground") // Asegúrate de tener una imagen con este nombre en tus Assets.xcassets
                    .resizable()
                    .scaledToFit()
                    .background(Color.gray.opacity(0.1))
            }
            .frame(height: 150)
            .clipped()

            Text(programa.name.unescaped()) // Usamos nuestra función para limpiar el título
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 8)
                .frame(minHeight: 50) // Damos una altura mínima para alinear títulos

        }
        .background(Color(UIColor.systemGray5)) // Un color de fondo sutil
        .cornerRadius(12)
    }
}