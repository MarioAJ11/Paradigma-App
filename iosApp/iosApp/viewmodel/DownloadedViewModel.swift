import Foundation
import shared
import Combine

/**
 * ViewModel que gestiona la lista de episodios descargados.
 * Se comunica con `DownloadManager` para iniciar descargas y se suscribe
 * a sus notificaciones para saber cuándo una descarga ha finalizado.
 */
@MainActor
class DownloadedViewModel: ObservableObject {

    @Published var downloadedEpisodios: [Episodio] = [] {
        didSet { saveDownloadsToUserDefaults() }
    }

    // Referencia al gestor de descargas y a sus eventos.
    private let downloadManager = DownloadManager.shared
    private var cancellables = Set<AnyCancellable>()
    private let sdk = AppServices.shared.sdk
    private let userDefaultsKey = "downloaded_episodes_list"

    init() {
        loadDownloadsFromUserDefaults()

        // Se suscribe a los eventos de descargas completadas del DownloadManager.
        downloadManager.downloadCompleted
            .receive(on: DispatchQueue.main)
            .sink { [weak self] episodio in
                self?.addEpisodioToList(episodio)
            }
            .store(in: &cancellables)
    }

    /// Inicia el proceso de descarga de un episodio a través del DownloadManager.
    func downloadEpisodio(_ episodio: Episodio) {
        downloadManager.startDownload(episodio: episodio)
    }

    /// Elimina un episodio de la lista de descargados Y su archivo físico.
    func removeDownload(episodio: Episodio) {
        deleteLocalFile(for: episodio) // Borra el archivo del disco.
        downloadedEpisodios.removeAll { $0.id == episodio.id }
    }

    /// Comprueba si un episodio está descargado (está en la lista).
    func isDownloaded(episodio: Episodio) -> Bool {
        return downloadedEpisodios.contains { $0.id == episodio.id }
    }

    /// Comprueba si un episodio se está descargando activamente.
    func isDownloading(episodio: Episodio) -> Bool {
        return downloadManager.isDownloading(episodio: episodio)
    }

    /// Añade un episodio a la lista de descargados (privado).
    private func addEpisodioToList(_ episodio: Episodio) {
        guard !isDownloaded(episodio: episodio) else { return }
        downloadedEpisodios.append(episodio)
    }


    // MARK: - Lógica de Persistencia

    /// Guarda la lista de episodios descargados en UserDefaults, convirtiéndolos a JSON.
    private func saveDownloadsToUserDefaults() {
        let encoder = JSONEncoder()
        // Usamos un 'do-catch' por si la codificación a JSON falla.
        do {
            let data = try encoder.encode(downloadedEpisodios)
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
        } catch {
            print("Error al guardar episodios descargados: \(error.localizedDescription)")
        }
    }

    /// Carga la lista de episodios descargados desde UserDefaults.
    private func loadDownloadsFromUserDefaults() {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey) else { return }

        let decoder = JSONDecoder()
        do {
            let episodios = try decoder.decode([Episodio].self, from: data)
            self.downloadedEpisodios = episodios
        } catch {
            print("Error al cargar episodios descargados: \(error.localizedDescription)")
        }
    }
}