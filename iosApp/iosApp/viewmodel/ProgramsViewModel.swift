import Foundation
import shared

/**
 * ViewModel para la pantalla principal (`HomeScreen`).
 *
 * Se encarga de cargar la lista de programas y gestionar los estados de carga y error,
 * utilizando la instancia compartida del SDK para todas las operaciones de datos.
 */
@MainActor
class ProgramsViewModel: ObservableObject {

    @Published var programs: [Programa] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil

    // Utiliza la instancia única del SDK proporcionada por AppServices.
    private let sdk = AppServices.shared.sdk

    /**
     * Carga la lista de programas desde el SDK de forma asíncrona.
     * Actualiza las propiedades publicadas (`Published`) para reflejar el estado en la UI.
     */
    func loadPrograms() async {
        guard programs.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        do {
            programs = try await asyncResult(for: sdk.getProgramas())
        } catch {
            errorMessage = "No se pudieron cargar los programas. Revisa tu conexión."
        }
        isLoading = false
    }
}