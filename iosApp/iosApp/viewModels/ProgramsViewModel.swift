import Foundation
import SwiftUI
import shared

/**
 * ViewModel para la pantalla principal que muestra la lista de programas.
 * Es un `ObservableObject`, lo que permite a las vistas de SwiftUI reaccionar a sus cambios.
 *
 * Se encarga de:
 * - Llamar al `ParadigmaSDK` para obtener los datos.
 * - Gestionar los estados de carga y error.
 * - "Publicar" la lista de programas para que la vista la pueda mostrar.
 */
@MainActor // Asegura que los cambios a las propiedades @Published se hagan en el hilo principal.
class ProgramsViewModel: ObservableObject {
    // La vista se actualizará automáticamente cuando estas propiedades cambien.

    @Published var programs: [Programa] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil

    // Creamos una instancia de nuestro SDK de Kotlin para acceder a la lógica compartida.
    private let sdk = ParadigmaSDK()

    /**
     * Carga la lista de programas desde el repositorio compartido de forma asíncrona.
     */
    func loadPrograms() async {
        self.isLoading = true
        self.errorMessage = nil

        do {
            // Usamos nuestra función 'asyncResult' para llamar a la función 'suspend' de Kotlin.
            let programsResult = try await asyncResult(for: sdk.getProgramas())
            // Si todo va bien, actualizamos la lista de programas.
            self.programs = programsResult
        } catch {
            // Si ocurre un error, lo guardamos para mostrarlo en la UI.
            self.errorMessage = error.localizedDescription
        }

        // Al finalizar, sea con éxito o error, indicamos que la carga ha terminado.
        self.isLoading = false
    }
}