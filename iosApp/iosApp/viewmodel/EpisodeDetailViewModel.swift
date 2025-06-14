import Foundation
import shared

/**
 * ViewModel para la pantalla que muestra los detalles completos de un episodio.
 *
 * Se encarga de:
 * - Recibir el ID de un episodio.
 * - Cargar todos los datos de ese episodio desde el SDK.
 * - Extraer los programas asociados a partir de los datos embebidos del episodio.
 * - Publicar los datos y los estados de carga/error para la vista de SwiftUI.
 */
@MainActor
class EpisodeDetailViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas
    
    /// El objeto Episodio con todos sus detalles. Es `nil` hasta que se carga.
    @Published var episodio: Episodio? = nil
    
    /// La lista de programas a los que pertenece este episodio.
    @Published var programasAsociados: [Programa] = []
    
    /// Indica si se está realizando la carga inicial de datos para la vista.
    @Published var isLoading = false
    
    /// Contiene un mensaje de error si la carga de datos falla.
    @Published var errorMessage: String? = nil

    // MARK: - Propiedades Privadas

    // Utiliza la instancia única y compartida del SDK.
    private let sdk = AppServices.shared.sdk
    
    /// El ID del episodio que este ViewModel está gestionando.
    private let episodioId: Int

    // MARK: - Inicializador

    /**
     * Inicializa el ViewModel con el ID del episodio a mostrar.
     * - Parameter episodioId: El identificador único del episodio.
     */
    init(episodioId: Int) {
        self.episodioId = episodioId
    }

    // MARK: - Métodos Públicos

    /// Carga los detalles completos del episodio desde el SDK.
    func loadDetails() async {
        // Evita recargar si los datos ya existen.
        guard self.episodio == nil else { return }

        self.isLoading = true
        self.errorMessage = nil

        do {
            // Llama a la función del SDK para obtener el episodio por su ID.
            if let fetchedEpisodio = try await asyncResult(for: sdk.getEpisodio(episodioId: Int32(episodioId))) {
                // Si se encuentra, lo publicamos para la vista.
                self.episodio = fetchedEpisodio
                // Extraemos los programas asociados desde los datos embebidos.
                self.extractAssociatedPrograms(from: fetchedEpisodio)
            } else {
                // Si el SDK devuelve null (ej. error 404), mostramos un mensaje.
                self.errorMessage = "No se pudo encontrar el episodio."
            }
        } catch {
            // Capturamos cualquier otro error de red o de la API.
            self.errorMessage = "Error al cargar los detalles: \(error.localizedDescription)"
        }

        self.isLoading = false
    }

    // MARK: - Métodos Privados

    /**
     * Procesa el objeto Episodio para extraer la lista de programas a los que pertenece.
     * La API de WordPress devuelve esta información en una estructura anidada (`_embedded`).
     * - Parameter from: El objeto `Episodio` del que se extraerán los programas.
     */
    private func extractAssociatedPrograms(from episodio: Episodio) {
        // La propiedad 'terms' es una lista de listas. Primero las aplanamos en una sola.
        let allTerms = episodio.embedded?.terms?.flatMap { $0 } ?? []

        if let programaIds = episodio.programaIds {
            // Creamos un Set de los IDs para una búsqueda más eficiente.
            let idSet = Set(programaIds.map { $0.int32Value })
            // Filtramos los términos para quedarnos solo con los programas correctos.
            self.programasAsociados = allTerms.filter { programa in
                idSet.contains(programa.id)
            }
        } else {
            self.programasAsociados = allTerms
        }
    }
}
