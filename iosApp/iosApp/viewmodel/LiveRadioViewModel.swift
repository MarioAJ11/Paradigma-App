import Foundation
import shared

/**
 * ViewModel para gestionar el estado y los metadatos de la radio en directo.
 */
@MainActor
class LiveRadioViewModel: ObservableObject {

    @Published var radioInfo: RadioInfo? = nil

    private let sdk = ParadigmaSDK()
    private var timer: Timer?

    init() {
        // Inicia un temporizador para refrescar la información de la radio cada 15 segundos.
        timer = Timer.scheduledTimer(withTimeInterval: 15.0, repeats: true) { [weak self] _ in
            Task {
                await self?.fetchRadioInfo()
            }
        }
        // Llama una vez al inicio.
        Task { await fetchRadioInfo() }
    }

    /// Obtiene la información más reciente de la radio desde el SDK.
    private func fetchRadioInfo() async {
        do {
            // Necesitamos una función en el SDK para esto.
            // Suponiendo que la añadimos, la llamaríamos aquí.
            // self.radioInfo = try await asyncResult(for: sdk.getRadioInfo())
            print("Función getRadioInfo() en el SDK aún no implementada.")
        } catch {
            print("Error al obtener info de la radio: \(error.localizedDescription)")
            self.radioInfo = nil
        }
    }

    deinit {
        // Detiene el temporizador cuando el ViewModel se destruye.
        timer?.invalidate()
    }
}