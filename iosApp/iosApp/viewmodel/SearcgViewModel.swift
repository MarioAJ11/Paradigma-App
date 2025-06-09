import Foundation
import shared
import Combine // Importamos Combine para el debounce

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

    private let sdk = ParadigmaSDK()

    /// Un objeto que nos permite cancelar subscripciones de Combine, como la del buscador.
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Inicializador

    init() {
        // Configuramos la lógica reactiva de la búsqueda.
        setupSearchPublisher()
    }

    // MARK: - Lógica de Búsqueda

    private func setupSearchPublisher() {
        $searchText
            // 1. Espera 500ms después de que el usuario deja de escribir (debounce).
            .debounce(for: .milliseconds(500), scheduler: RunLoop.main)
            // 2. Solo procede si el texto ha cambiado.
            .removeDuplicates()
            // 3. Solo busca si el texto tiene 3 o más caracteres.
            .filter { $0.count >= 3 }
            // 4. Inicia la búsqueda.
            .sink { [weak self] searchText in
                Task {
                    await self?.performSearch(query: searchText)
                }
            }
            .store(in: &cancellables)

        // Limpia los resultados si el texto se borra o es muy corto.
        $searchText
            .filter { $0.count < 3 }
            .sink { [weak self] _ in
                self?.searchResults = []
                self?.infoMessage = "Escribe al menos 3 caracteres para buscar."
            }
            .store(in: &cancellables)
    }

    /**
     * Ejecuta la búsqueda de forma asíncrona usando el SDK.
     * - Parameter query: El texto a buscar.
     */
    private func performSearch(query: String) async {
        self.isLoading = true
        self.infoMessage = nil

        do {
            let results = try await asyncResult(for: sdk.buscarEpisodios(searchTerm: query))
            self.searchResults = results

            if results.isEmpty {
                self.infoMessage = "No se encontraron resultados para \"\(query)\"."
            }

        } catch {
            self.infoMessage = "Error en la búsqueda: \(error.localizedDescription)"
        }

        self.isLoading = false
    }
}