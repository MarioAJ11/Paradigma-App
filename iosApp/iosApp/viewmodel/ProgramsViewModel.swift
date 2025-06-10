import Foundation
import shared

/**
 * ViewModel para la pantalla principal (`HomeScreen`).
 *
 * Se encarga de:
 * - Cargar la lista completa de programas desde el `ParadigmaSDK`.
 * - Publicar la lista de programas para que la vista `HomeScreen` la muestre.
 * - Gestionar los estados de carga y error durante la obtención de los datos.
 */
@MainActor
class ProgramsViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas

    /// La lista de programas que se mostrará en la cuadrícula principal.
    @Published var programs: [Programa] = []

    /// Indica si se está realizando la carga inicial de datos.
    @Published var isLoading = false

    /// Contiene un mensaje de error si la carga falla.
    @Published var errorMessage: String? = nil

    // MARK: - Propiedades Privadas

    private let sdk: ParadigmaSDK

    // MARK: - Inicializador

    init() {
        // Inicializamos el SDK con su repositorio, creando las dependencias de la base de datos.
        let databaseDriverFactory = DatabaseDriverFactory()
        let database = Database(databaseDriverFactory: databaseDriverFactory)
        let repository = ParadigmaRepository(database: database)
        self.sdk = ParadigmaSDK(repository: repository)
    }


    // MARK: - Métodos Públicos

    /// Carga la lista completa de programas desde el SDK.
    func loadPrograms() async {
        // Evita recargar si ya hay programas cargados.
        // Para forzar una recarga, se podría añadir un parámetro `forceReload: Bool = false`.
        guard programs.isEmpty else { return }

        isLoading = true
        errorMessage = nil

        do {
            // Llama a la función del SDK para obtener los programas y los publica.
            programs = try await asyncResult(for: sdk.getProgramas())
        } catch {
            // Si ocurre un error, se guarda el mensaje para mostrarlo en la UI.
            errorMessage = "No se pudieron cargar los programas. Revisa tu conexión. (\(error.localizedDescription))"
        }

        isLoading = false
    }
}