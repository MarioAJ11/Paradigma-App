import Foundation
import shared

/**
 * ViewModel que gestiona la cola de reproducción de episodios.
 *
 * Mantiene una lista de IDs de episodios, los guarda y carga de `UserDefaults`
 * para que la cola persista entre sesiones de la app.
 */
@MainActor
class QueueViewModel: ObservableObject {

    // MARK: - Propiedades Publicadas

    /// La lista de objetos Episodio completos en la cola, listos para mostrar en la UI.
    @Published var episodiosEnCola: [Episodio] = []

    /// Los IDs de los episodios en la cola. Es la "fuente de la verdad".
    @Published private var idEpisodios: [Int32] = [] {
        didSet {
            // Cada vez que los IDs cambian, los guardamos y actualizamos la lista de episodios.
            saveQueueToUserDefaults()
            Task {
                await fetchEpisodioDetails()
            }
        }
    }

    // MARK: - Propiedades Privadas

    private let sdk = ParadigmaSDK()
    private let userDefaultsKey = "episode_queue_ids"

    // MARK: - Inicializador

    init() {
        // Al iniciar, cargamos la cola que se guardó en la sesión anterior.
        loadQueueFromUserDefaults()
    }

    // MARK: - Métodos Públicos

    /// Añade un episodio al final de la cola si no está ya presente.
    func addEpisodio(episodio: Episodio) {
        guard !idEpisodios.contains(episodio.id) else { return }
        idEpisodios.append(episodio.id)
    }

    /// Elimina un episodio de la cola.
    func removeEpisodio(episodio: Episodio) {
        idEpisodios.removeAll { $0 == episodio.id }
    }

    /// Comprueba si un episodio está en la cola.
    func isInQueue(episodio: Episodio) -> Bool {
        return idEpisodios.contains(episodio.id)
    }

    // MARK: - Lógica de Persistencia y Carga

    /// Guarda la lista de IDs en las preferencias del usuario.
    private func saveQueueToUserDefaults() {
        UserDefaults.standard.set(idEpisodios, forKey: userDefaultsKey)
    }

    /// Carga la lista de IDs desde las preferencias del usuario.
    private func loadQueueFromUserDefaults() {
        // El casting a [Int32] es necesario porque UserDefaults devuelve [Any].
        self.idEpisodios = UserDefaults.standard.array(forKey: userDefaultsKey) as? [Int32] ?? []
    }

    /// Obtiene los detalles completos de los episodios a partir de sus IDs.
    private func fetchEpisodioDetails() async {
        var apgEpisodios: [Episodio] = []
        for id in idEpisodios {
            if let episodio = try? await asyncResult(for: sdk.getEpisodio(episodioId: id)) {
                apgEpisodios.append(episodio)
            }
        }
        self.episodiosEnCola = apgEpisodios
    }
}