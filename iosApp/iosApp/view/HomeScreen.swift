import SwiftUI

/**
 * Vista principal de la app que muestra la cuadrícula de programas disponibles.
 * Gestiona los estados de carga, error y contenido vacío.
 */
struct HomeScreen: View {
    // Usamos el ViewModel que ya existe para esta pantalla.
    @StateObject private var viewModel = ProgramsViewModel()

    // Definimos las columnas para la cuadrícula (2 columnas con espaciado).
    private let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16)
    ]

    var body: some View {
        // NavigationView permite la navegación a otras vistas.
        NavigationView {
            ZStack {
                // El ZStack nos permite superponer vistas, como el indicador de carga.
                if viewModel.isLoading && viewModel.programs.isEmpty {
                    // Muestra un indicador de progreso si está cargando y no hay datos.
                    ProgressView("Cargando programas...")
                } else if let errorMessage = viewModel.errorMessage {
                    // Muestra un mensaje de error si la carga falló.
                    VStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.largeTitle)
                            .foregroundColor(.red)
                        Text(errorMessage)
                            .multilineTextAlignment(.center)
                            .padding()
                        Button("Reintentar") {
                            Task { await viewModel.loadPrograms() }
                        }
                    }
                } else if viewModel.programs.isEmpty {
                    Text("No hay programas disponibles.")
                        .foregroundColor(.secondary)
                } else {
                    // Muestra la cuadrícula de programas si todo ha ido bien.
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(viewModel.programs, id: \.id) { programa in
                                // NavigationLink nos llevará a la vista de detalle del programa.
                                // Por ahora, el destino es un simple texto.
                                NavigationLink(destination: Text("Detalle de \(programa.name)")) {
                                    ProgramaListItemView(programa: programa)
                                }
                                .buttonStyle(.plain) // Evita que toda la celda se pinte de azul.
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Programas") // Título de la pantalla.
            .onAppear {
                // Cuando la vista aparece, le pide al ViewModel que cargue los datos.
                Task {
                    await viewModel.loadPrograms()
                }
            }
        }
    }
}