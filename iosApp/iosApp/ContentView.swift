import SwiftUI
import shared

struct ContentView: View {
    // Con @StateObject, creamos y mantenemos viva una instancia de nuestro ViewModel
    // mientras la vista esté activa.
    @StateObject private var viewModel = ProgramsViewModel()

    var body: some View {
        NavigationView {
            // La vista cambia dependiendo del estado del ViewModel
            VStack {
                if viewModel.isLoading {
                    // Muestra un indicador de progreso mientras carga
                    ProgressView("Cargando programas...")
                } else if let errorMessage = viewModel.errorMessage {
                    // Muestra un mensaje de error si algo falló
                    Text("Error: \(errorMessage)")
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding()
                } else {
                    // Muestra la lista de programas si todo fue bien
                    List(viewModel.programs, id: \.id) { programa in
                        ProgramRow(programa: programa)
                            .listRowInsets(EdgeInsets()) // Quita los paddings por defecto de la fila
                            .padding(.vertical, 4)
                    }
                    .listStyle(.plain) // Un estilo de lista simple
                }
            }
            .navigationTitle("Programas") // Título en la barra de navegación
            .onAppear {
                // Cuando la vista aparece por primera vez, le pide al ViewModel que cargue los datos.
                viewModel.loadPrograms()
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}