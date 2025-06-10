import SwiftUI
import shared

/**
 * Vista principal de la app que muestra la cuadrícula de programas disponibles.
 * Gestiona los estados de carga, error y contenido vacío.
 */
struct HomeScreen: View {
    @StateObject private var viewModel = ProgramsViewModel()

    private let columns = [
        GridItem(.flexible(), spacing: 16),
        GridItem(.flexible(), spacing: 16)
    ]

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading && viewModel.programs.isEmpty {
                    ProgressView("Cargando programas...")
                } else if let errorMessage = viewModel.errorMessage {
                    VStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.largeTitle).foregroundColor(.red)
                        Text(errorMessage).multilineTextAlignment(.center).padding()
                        Button("Reintentar") { Task { await viewModel.loadPrograms() } }
                    }
                } else if viewModel.programs.isEmpty {
                    Text("No hay programas disponibles.").foregroundColor(.secondary)
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(viewModel.programs, id: \.id) { programa in
                                // El NavigationLink ahora apunta a la vista de detalle real.
                                NavigationLink(destination: ProgramaDetailView(programa: programa)) {
                                    ProgramaListItemView(programa: programa)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Programas")
            .onAppear {
                Task { await viewModel.loadPrograms() }
            }
        }
    }
}