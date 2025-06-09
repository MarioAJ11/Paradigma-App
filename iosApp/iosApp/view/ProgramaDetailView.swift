import SwiftUI
import shared

/**
 * Vista que muestra los detalles de un programa y una lista paginada de sus episodios.
 * Ensambla la cabecera (`ProgramaInfoHeader`) y la lista (`EpisodiosListView`).
 */
struct ProgramaDetailView: View {

    // MARK: - Propiedades

    @StateObject private var viewModel: ProgramaViewModel

    // MARK: - Inicializador

    init(programa: Programa) {
        _viewModel = StateObject(wrappedValue: ProgramaViewModel(programaId: Int(programa.id)))
    }

    // MARK: - Cuerpo de la Vista

    var body: some View {
        ScrollView {
            VStack {
                // Muestra un indicador de carga hasta que los datos estén listos.
                if viewModel.isLoading && viewModel.programa == nil {
                    ProgressView().padding(.top, 100)

                // Muestra el contenido principal cuando los datos se han cargado.
                } else if let programa = viewModel.programa {
                    ProgramaInfoHeader(programa: programa)
                    EpisodiosListView(viewModel: viewModel)

                // Muestra un mensaje si ha ocurrido un error.
                } else if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage).foregroundColor(.red).padding()
                }
            }
        }
        .onAppear {
            Task { await viewModel.loadInitialData() }
        }
        .navigationTitle(viewModel.programa?.name.unescaped() ?? "Cargando...")
        .navigationBarTitleDisplayMode(.inline)
    }
}