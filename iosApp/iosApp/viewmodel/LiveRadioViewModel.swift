import Foundation
import shared

/**
 * ViewModel para gestionar el estado y los metadatos de la radio en directo.
 * Ahora usa la instancia compartida del SDK.
 */
@MainActor
class LiveRadioViewModel: ObservableObject {

    @Published var radioInfo: RadioInfo? = nil

    // Usa la instancia única del SDK.
    private let sdk = AppServices.shared.sdk
    private var timer: Timer?

    init() {
        // Inicia un temporizador para refrescar la información.
        timer = Timer.scheduledTimer(withTimeInterval: 15.0, repeats: true) { [weak self] _ in
            Task {
                await self?.fetchRadioInfo()
            }
        }
        // Llama una vez al inicio.
        Task { await fetchRadioInfo() }
    }

    /// Obtiene la información más reciente de la radio.
    private func fetchRadioInfo() async {
        do {
            self.radioInfo = try await asyncResult(for: sdk.getRadioInfo())
        } catch {
            print("Error al obtener info de la radio: \(error.localizedDescription)")
            self.radioInfo = nil
        }
    }

    deinit {
        timer?.invalidate()
    }
}