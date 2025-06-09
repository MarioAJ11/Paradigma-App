import Foundation
import shared
import SwiftUI

/**
 * ViewModel que gestiona la lista de episodios cuya reproducción está "en curso" o pausada.
 * Guarda y carga el progreso de múltiples episodios usando UserDefaults.
 */
@MainActor
class OnGoingViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas

    /// La lista de episodios en curso, ordenados por el más reciente.
    @Published var onGoingEpisodios: [Episodio] = []

    // MARK: - Propiedades Privadas

    /// El diccionario que se guarda en UserDefaults: [ID_Episodio: Progreso_en_segundos]
    @AppStorage("on_going_progress_map") private var progressMapData: Data = Data()

    private let sdk = ParadigmaSDK()

    // MARK: - Métodos Públicos

    /**
     * Carga y actualiza la lista de episodios en curso a partir de los datos guardados.
     */
    func loadOnGoingEpisodios() async {
        let progressMap = decodeProgressMap()
        var fetchedEpisodios: [Episodio] = []

        // No cargamos episodios cuyo progreso sea 0.
        let validIds = progressMap.filter { $0.value > 0 }.keys

        for id in validIds {
            if let episodio = try? await asyncResult(for: sdk.getEpisodio(episodioId: Int32(id))) {
                fetchedEpisodios.append(episodio)
            }
        }

        self.onGoingEpisodios = fetchedEpisodios
    }

    /**
     * Guarda el progreso de un episodio.
     * - Parameters:
     * - episodioId: El ID del episodio.
     * - progressSeconds: El segundo actual de la reproducción.
     */
    func saveProgress(episodioId: Int, progressSeconds: Double) {
        var progressMap = decodeProgressMap()
        progressMap[episodioId] = progressSeconds
        encodeAndSave(progressMap)
    }

    // MARK: - Lógica de Persistencia

    private func decodeProgressMap() -> [Int: Double] {
        guard let map = try? JSONDecoder().decode([Int: Double].self, from: progressMapData) else {
            return [:]
        }
        return map
    }

    private func encodeAndSave(_ map: [Int: Double]) {
        if let data = try? JSONEncoder().encode(map) {
            self.progressMapData = data
        }
    }
}