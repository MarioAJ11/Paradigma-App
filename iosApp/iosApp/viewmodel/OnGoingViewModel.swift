import Foundation
import shared
import SwiftUI

/**
 * ViewModel que gestiona la lista de episodios cuya reproducción está "en curso" o pausada.
 * Guarda y carga el progreso de múltiples episodios usando UserDefaults.
 */
@MainActor
class OnGoingViewModel: ObservableObject {

    @Published var onGoingEpisodios: [Episodio] = []

    @AppStorage("on_going_progress_map") private var progressMapData: Data = Data()

    // Se utiliza la instancia compartida del SDK en lugar de crear una nueva.
    private let sdk = AppServices.shared.sdk

    /**
     * Carga y actualiza la lista de episodios en curso a partir de los datos guardados.
     */
    func loadOnGoingEpisodios() async {
        let progressMap = decodeProgressMap()
        var fetchedEpisodios: [Episodio] = []
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
     */
    func saveProgress(episodioId: Int, progressSeconds: Double) {
        var progressMap = decodeProgressMap()
        progressMap[episodioId] = progressSeconds
        encodeAndSave(progressMap)
    }

    // El resto de la clase (decode, encode) se mantiene igual.
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
