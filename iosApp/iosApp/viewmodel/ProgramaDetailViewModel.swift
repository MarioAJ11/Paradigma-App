import Foundation
import shared

/**
 * ViewModel para la pantalla de detalle de un programa.
 *
 * Carga la información del programa y una lista paginada de sus episodios.
 * Gestiona el estado de carga y la paginación infinita (cargar más al hacer scroll).
 */
@MainActor
class ProgramaDetailViewModel: ObservableObject {

    @Published var programa: Programa? = nil
    @Published var episodios: [Episodio] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var errorMessage: String? = nil

    // Utiliza la instancia única del SDK.
    private let sdk = AppServices.shared.sdk
    private let programaId: Int32
    private var currentPage = 1
    private var canLoadMore = true

    init(programaId: Int32) {
        self.programaId = programaId
    }

    /**
     * Carga los datos iniciales: detalles del programa y la primera página de episodios.
     */
    func loadInitialData() async {
        guard episodios.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        do {
            // Carga los detalles del programa y la primera página de episodios en paralelo.
            async let programaDetails = asyncResult(for: sdk.getPrograma(programaId: self.programaId))
            async let firstPageEpisodios = fetchEpisodios(page: 1)

            self.programa = try await programaDetails
            self.episodios = try await firstPageEpisodios
        } catch {
            self.errorMessage = "Error al cargar los datos del programa."
        }
        isLoading = false
    }

    /**
     * Carga la siguiente página de episodios si es necesario y posible.
     * Se llama cuando el usuario se acerca al final de la lista.
     */
    func loadMoreEpisodiosIfNeeded(currentEpisodio: Episodio?) {
        guard let currentEpisodio = currentEpisodio,
              !isLoadingMore,
              canLoadMore,
              let lastEpisodio = episodios.last,
              lastEpisodio.id == currentEpisodio.id else {
            return
        }

        Task {
            isLoadingMore = true
            currentPage += 1
            do {
                let newEpisodios = await fetchEpisodios(page: currentPage)
                if newEpisodios.isEmpty {
                    canLoadMore = false
                } else {
                    self.episodios.append(contentsOf: newEpisodios)
                }
            }
            isLoadingMore = false
        }
    }

    /**
     * Función auxiliar para obtener una página de episodios desde el SDK.
     */
    private func fetchEpisodios(page: Int) async -> [Episodio] {
        do {
            return try await asyncResult(for: sdk.getEpisodiosPorPrograma(programaId: self.programaId, page: Int32(page)))
        } catch {
            // Si falla la carga de una página, se asume que no se puede cargar más.
            canLoadMore = false
            return []
        }
    }
}