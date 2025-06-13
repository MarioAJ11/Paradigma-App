import Foundation
import shared
import Combine

/**
 * ViewModel para la pantalla de búsqueda.
 *
 * Gestiona la lógica de búsqueda, incluyendo un retardo (debounce) para optimizar
 * las llamadas a la API mientras el usuario escribe.
 */
@MainActor
class SearchViewModel: ObservableObject {

    @Published var searchText = ""
    @Published var searchResults: [Episodio] = []
    @Published var isLoading = false
    @Published var infoMessage: String? = "Escribe al menos 3 caracteres para buscar."

    // Utiliza la instancia única del SDK.
    private let sdk = AppServices.shared.sdk
    private var cancellables = Set<AnyCancellable>()

    init() {
        setupSearchPublisher()
    }

    /**
     * Configura un publicador de Combine para reaccionar a los cambios en `searchText`
     * con un retardo, evitando búsquedas excesivas.
     */
    private func setupSearchPublisher() {
        $searchText
            .debounce(for: .milliseconds(500), scheduler: RunLoop.main)
            .removeDuplicates()
            .filter { $0.count >= 3 }
            .sink { [weak self] query in
                Task { await self?.performSearch(query: query) }
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

    /**
     * Ejecuta la búsqueda de episodios en la red a través del SDK.
     */
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