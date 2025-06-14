import Foundation
import Combine
@preconcurrency import shared

/**
 * Gestiona las descargas de archivos de audio.
 * Es un Singleton compatible con las estrictas reglas de concurrencia de Swift.
 */
@MainActor
class DownloadManager: NSObject, ObservableObject, URLSessionDownloadDelegate {

    static let shared = DownloadManager()

    let downloadCompleted = PassthroughSubject<Episodio, Never>()
    @Published var activeDownloads: [Int: Episodio] = [:]

    private lazy var urlSession: URLSession = {
        let config = URLSessionConfiguration.default
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }()

    func startDownload(episodio: Episodio) {
        guard let urlString = episodio.archiveUrl, let url = URL(string: urlString) else { return }
        let downloadTask = urlSession.downloadTask(with: url)
        activeDownloads[downloadTask.taskIdentifier] = episodio
        downloadTask.resume()
    }
    
    /**
     * Comprueba si un episodio se está descargando activamente.
     */
    func isDownloading(episodio: Episodio) -> Bool {
        // Busca si algún valor en el diccionario tiene el mismo ID que el episodio.
        return activeDownloads.values.contains { $0.id.int32Value == episodio.id.int32Value }
    }

    // MARK: - Delegate Methods

    // Se marca como 'nonisolated' porque el sistema lo llama desde un hilo de fondo.
    nonisolated func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        let taskIdentifier = downloadTask.taskIdentifier
        Task {
            await MainActor.run { // Volvemos al hilo principal para modificar propiedades @Published.
                guard let episodio = self.activeDownloads[taskIdentifier],
                      let destinationURL = localFilePath(for: episodio) else { return }
                
                let fileManager = FileManager.default
                try? fileManager.removeItem(at: destinationURL)

                do {
                    try fileManager.moveItem(at: location, to: destinationURL)
                    self.activeDownloads.removeValue(forKey: taskIdentifier)
                    self.downloadCompleted.send(episodio)
                } catch {
                    self.activeDownloads.removeValue(forKey: taskIdentifier)
                }
            }
        }
    }

    // También se marca como 'nonisolated'.
    nonisolated func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        let taskIdentifier = task.taskIdentifier
        if error != nil {
            Task {
                await MainActor.run { // Volvemos al hilo principal.
                    self.activeDownloads.removeValue(forKey: taskIdentifier)
                }
            }
        }
    }
}
