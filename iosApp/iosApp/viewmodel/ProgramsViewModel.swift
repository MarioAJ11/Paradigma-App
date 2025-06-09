import Foundation
import shared // Importamos el módulo compartido de Kotlin

/**
 * ViewModel para la pantalla que muestra los detalles de un programa y su lista de episodios.
 *
 * Se encarga de:
 * - Recibir el ID de un programa.
 * - Cargar los detalles de ese programa (nombre, imagen, etc.).
 * - Cargar la lista de sus episodios de forma paginada (infite scroll).
 * - Publicar los datos y los estados de carga/error para que la vista de SwiftUI los muestre.
 */
@MainActor // Asegura que los cambios a las propiedades @Published se hagan en el hilo principal.
class ProgramaViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas

    /// Los detalles del programa que se está mostrando. `null` inicialmente.
    @Published var programa: Programa? = nil

    /// La lista de episodios cargados para este programa.
    @Published var episodios: [Episodio] = []

    /// Indica si se está realizando una carga de datos (inicial o paginada).
    @Published var isLoading = false

    /// Contiene un mensaje de error si alguna operación falla.
    @Published var errorMessage: String? = nil

    // MARK: - Propiedades Privadas

    private let sdk = ParadigmaSDK()

    /// El ID del programa que este ViewModel está gestionando.
    private let programaId: Int

    /// El estado de la paginación.
    private var currentPage = 1
    private var canLoadMore = true
    private var isLoadingMore = false

    // MARK: - Inicializador

    init(programaId: Int) {
        self.programaId = programaId
    }

    // MARK: - Métodos Públicos

    /// Carga los datos iniciales: los detalles del programa y la primera página de episodios.
    func loadInitialData() async {
        // Solo ejecuta la carga inicial si aún no se ha hecho.
        guard self.programa == nil else { return }

        self.isLoading = true
        self.errorMessage = nil

        do {
            // Carga los detalles del programa para la cabecera.
            self.programa = try await asyncResult(for: sdk.getPrograma(programaId: Int32(programaId)))

            // Carga la primera página de episodios.
            let firstPageEpisodios = try await fetchEpisodios(page: 1)
            self.episodios = firstPageEpisodios

        } catch {
            // Si algo falla, guardamos el mensaje de error.
            self.errorMessage = "Error al cargar el programa: \(error.localizedDescription)"
        }

        self.isLoading = false
    }

    /// Carga la siguiente página de episodios.
    /// La vista llamará a esta función cuando el usuario llegue al final de la lista.
    func loadMoreEpisodios() async {
        // Evita cargas múltiples si ya hay una en curso o si no hay más páginas.
        guard canLoadMore, !isLoadingMore else { return }

        self.isLoadingMore = true

        do {
            let newEpisodios = try await fetchEpisodios(page: self.currentPage)

            // Añade los nuevos episodios a la lista existente.
            self.episodios.append(contentsOf: newEpisodios)

        } catch {
            // En caso de error, simplemente dejamos de cargar más.
            // No mostramos un error para no interrumpir la vista de los ya cargados.
            self.canLoadMore = false
        }

        self.isLoadingMore = false
    }

    // MARK: - Métodos Privados

    /// Función auxiliar para obtener episodios de una página específica desde el SDK.
    private func fetchEpisodios(page: Int) async throws -> [Episodio] {
        let fetchedEpisodios = try await asyncResult(for: sdk.getEpisodiosPorPrograma(programaId: Int32(programaId), page: Int32(page)))

        // Si la API devuelve menos episodios de los que pedimos (o 0),
        // asumimos que hemos llegado al final.
        if fetchedEpisodios.count < 20 {
            self.canLoadMore = false
        }

        // Incrementamos el contador para la próxima llamada.
        self.currentPage += 1

        return fetchedEpisodios
    }
}