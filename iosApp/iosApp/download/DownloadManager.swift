import Foundation
import Combine
import shared

/**
 * Gestiona las descargas de los archivos de audio de los episodios.
 * Utiliza URLSession para realizar descargas en segundo plano.
 * Es un Singleton ObservableObject que puede notificar a la app cuando una descarga se completa.
 */
@MainActor
class DownloadManager: NSObject, ObservableObject, URLSessionDownloadDelegate {

    static let shared = DownloadManager()

    /// Publica el Episodio cuya descarga se acaba de completar con éxito.
    let downloadCompleted = PassthroughSubject<Episodio, Never>()

    /// Mantiene un registro de las descargas activas. [ID de Tarea: Episodio]
    @Published var activeDownloads: [Int: Episodio] = [:]

    private lazy var urlSession: URLSession = {
        let config = URLSessionConfiguration.default
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }()

    /// Inicia la descarga de un episodio.
    func startDownload(episodio: Episodio) {
        guard let urlString = episodio.archiveUrl, let url = URL(string: urlString) else { return }
        let downloadTask = urlSession.downloadTask(with: url)
        activeDownloads[downloadTask.taskIdentifier] = episodio
        downloadTask.resume()
    }

    /// Comprueba si un episodio se está descargando activamente.
    func isDownloading(episodio: Episodio) -> Bool {
        return activeDownloads.values.contains { $0.id == episodio.id }
    }

    // MARK: - Delegate Methods

    /// Se llama cuando una descarga finaliza con éxito.
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        guard let episodio = activeDownloads[downloadTask.taskIdentifier],
              let destinationURL = localFilePath(for: episodio) else { return }

        let fileManager = FileManager.default
        try? fileManager.removeItem(at: destinationURL)

        do {
            try fileManager.moveItem(at: location, to: destinationURL)
            // Ejecuta el código de UI en el hilo principal de forma segura.
            Task {
                activeDownloads.removeValue(forKey: downloadTask.taskIdentifier)
                downloadCompleted.send(episodio)
            }
        } catch {
            print("Error al mover el archivo: \(error.localizedDescription)")
            Task {
                activeDownloads.removeValue(forKey: downloadTask.taskIdentifier)
            }
        }
    }

    /// Se llama si ocurre un error durante la descarga.
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            print("Error en la descarga: \(error.localizedDescription)")
            Task {
                activeDownloads.removeValue(forKey: task.taskIdentifier)
            }
        }
    }
}