import Foundation
import shared
import Combine

/**
 * ViewModel para la pantalla de búsqueda.
 * Gestiona el texto de búsqueda, realiza la llamada a la API y publica los resultados.
 * Implementa un "debounce" para evitar realizar búsquedas con cada letra que escribe el usuario.
 */
@MainActor
class SearchViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas

    /// El texto actual en el campo de búsqueda, vinculado a la UI.
    @Published var searchText = ""

    /// La lista de episodios encontrados que la vista mostrará.
    @Published var searchResults: [Episodio] = []

    /// Indica si una búsqueda está en curso.
    @Published var isLoading = false

    /// Muestra mensajes informativos en la vista (ej: "Escribe para buscar", "No hay resultados").
    @Published var infoMessage: String? = "Escribe al menos 3 caracteres para buscar."

    // MARK: - Propiedades Privadas

    private let sdk: ParadigmaSDK
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Inicializador

    init() {
        // Inicializamos el SDK con su repositorio, creando las dependencias de la base de datos.
        let databaseDriverFactory = DatabaseDriverFactory()
        let database = Database(databaseDriverFactory: databaseDriverFactory)
        let repository = ParadigmaRepository(database: database)
        self.sdk = ParadigmaSDK(repository: repository)

        setupSearchPublisher()
    }

    // MARK: - Lógica de Búsqueda

    private func setupSearchPublisher() {
        $searchText
            .debounce(for: .milliseconds(400), scheduler: RunLoop.main)
            .removeDuplicates()
            .filter { $0.count >= 3 }
            .sink { [weak self] searchText in
                Task {
                    await self?.performSearch(query: searchText)
                }
            }
            .store(in: &cancellables)

        $searchText
            .filter { $0.count < 3 }
            .sink { [weak self] _ in
                self?.searchResults = []
                self?.infoMessage = "Escribe al menos 3 caracteres para buscar."
            }
            .store(in: &cancellables)
    }

    private func performSearch(query: String) async {
        isLoading = true
        infoMessage = nil

        do {
            let results = try await asyncResult(for: sdk.buscarEpisodios(searchTerm: query))
            self.searchResults = results

            if results.isEmpty {
                self.infoMessage = "No se encontraron resultados para \"\(query)\"."
            }
        } catch {
            self.infoMessage = "Error en la búsqueda: \(error.localizedDescription)"
        }

        isLoading = false
    }
}