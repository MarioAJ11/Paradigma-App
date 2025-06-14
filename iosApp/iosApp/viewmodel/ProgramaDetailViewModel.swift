import Foundation
import shared

/**
 * ViewModel para la pantalla de detalle de un programa.
 *
 * Se encarga de:
 * - Cargar la información del programa (título, imagen, etc.).
 * - Cargar una lista paginada de los episodios de ese programa.
 * - Gestionar la carga de más episodios a medida que el usuario se desplaza (scroll infinito).
 */
@MainActor
class ProgramaDetailViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas
    @Published var programa: Programa? = nil
    @Published var episodios: [Episodio] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var errorMessage: String? = nil

    // MARK: - Propiedades Privadas
    private let sdk = AppServices.shared.sdk
    private let programaId: Int32
    private var currentPage = 1
    private var canLoadMore = true

    // MARK: - Inicializador
    init(programaId: Int32) {
        self.programaId = programaId
    }

    // MARK: - Lógica de Carga de Datos
    
    /**
     * Carga los datos iniciales: los detalles del programa y la primera página de episodios.
     */
    func loadInitialData() async {
        guard episodios.isEmpty, !isLoading else { return }
        
        isLoading = true
        errorMessage = nil
        
        do {
            // Carga los detalles del programa y la primera página de episodios en paralelo.
            async let programaDetails = sdk.getPrograma(programaId: self.programaId)
            async let firstPageEpisodios = sdk.getEpisodiosPorPrograma(programaId: self.programaId, page: 1)
            
            self.programa = try await asyncResult(for: programaDetails)
            let newEpisodios = try await asyncResult(for: firstPageEpisodios)
            
            self.episodios = newEpisodios
            // Si la primera página devuelve menos de 20 episodios, asumimos que no hay más.
            if newEpisodios.count < 20 {
                self.canLoadMore = false
            }
            
        } catch {
            self.errorMessage = "Error al cargar los datos del programa."
        }
        
        isLoading = false
    }

    /**
     * Carga la siguiente página de episodios si es necesario y posible.
     * Se llama desde la vista cuando el usuario se acerca al final de la lista.
     * - Parameter currentEpisodio: El episodio que se está mostrando actualmente en la lista.
     */
    func loadMoreEpisodiosIfNeeded(currentEpisodio: Episodio?) {
        // Condiciones para cargar más:
        // 1. No estar ya cargando más.
        // 2. Haber más páginas por cargar.
        // 3. El episodio actual debe ser el último de la lista actual.
        guard !isLoadingMore, canLoadMore, let currentEpisodio = currentEpisodio, currentEpisodio.id == episodios.last?.id else {
            return
        }
        
        Task {
            isLoadingMore = true
            currentPage += 1
            
            do {
                let newEpisodios = try await asyncResult(for: sdk.getEpisodiosPorPrograma(programaId: self.programaId, page: Int32(currentPage)))
                
                // Si la nueva página está vacía, ya no se puede cargar más.
                if newEpisodios.isEmpty {
                    canLoadMore = false
                } else {
                    // Añade los nuevos episodios a la lista existente.
                    self.episodios.append(contentsOf: newEpisodios)
                }
            } catch {
                // Si una página falla, dejamos de intentar cargar más.
                self.canLoadMore = false
                print("Error al cargar más episodios: \(error.localizedDescription)")
            }
            
            isLoadingMore = false
        }
    }
}
