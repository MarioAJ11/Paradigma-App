import Foundation
import shared

/**
 * ViewModel que gestiona la cola de reproducción de episodios.
 */
@MainActor
class QueueViewModel: ObservableObject {

    @Published var episodiosEnCola: [Episodio] = []
    
    @Published private var idEpisodios: [Int32] = [] {
        didSet {
            saveQueueToUserDefaults()
            Task { await fetchEpisodioDetails() }
        }
    }

    private let sdk = AppServices.shared.sdk
    private let userDefaultsKey = "episode_queue_ids"

    init() {
        loadQueueFromUserDefaults()
    }

    /// Añade un episodio al final de la cola si no está ya presente.
    func addEpisodio(episodio: Episodio) {
        // Se convierte el ID de Kotlin a Int32 de Swift antes de comparar y añadir.
        let swiftId = episodio.id.int32Value
        guard !idEpisodios.contains(swiftId) else { return }
        idEpisodios.append(swiftId)
    }

    /// Elimina un episodio de la cola.
    func removeEpisodio(episodio: Episodio) {
        let swiftId = episodio.id.int32Value
        idEpisodios.removeAll { $0 == swiftId }
    }

    /// Comprueba si un episodio está en la cola.
    func isInQueue(episodio: Episodio) -> Bool {
        let swiftId = episodio.id.int32Value
        return idEpisodios.contains(swiftId)
    }

    private func saveQueueToUserDefaults() {
        UserDefaults.standard.set(idEpisodios, forKey: userDefaultsKey)
    }

    private func loadQueueFromUserDefaults() {
        self.idEpisodios = UserDefaults.standard.array(forKey: userDefaultsKey) as? [Int32] ?? []
    }

    private func fetchEpisodioDetails() async {
        var fetchedEpisodios: [Episodio] = []
        let idsToFetch = self.idEpisodios
        for id in idsToFetch {
            if let episodio = try? await asyncResult(for: sdk.getEpisodio(episodioId: id)) {
                fetchedEpisodios.append(episodio)
            }
        }
        self.episodiosEnCola = fetchedEpisodios
    }
}
