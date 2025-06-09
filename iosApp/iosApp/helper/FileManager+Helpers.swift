import Foundation
import shared

/**
 * Devuelve la URL local donde se debe guardar o desde donde se debe leer un episodio.
 * - Parameter episodio: El episodio para el cual se busca la ruta.
 * - Returns: Una URL en el directorio de Documentos de la app, o nil si hay un error.
 */
func localFilePath(for episodio: Episodio) -> URL? {
    // Obtenemos la URL del directorio de Documentos de la app, un lugar seguro para guardar datos.
    guard let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
        return nil
    }
    // Creamos un nombre de archivo único para el episodio para evitar colisiones.
    let fileName = "\(episodio.id)-\(episodio.slug).mp3"
    return documentsDirectory.appendingPathComponent(fileName)
}

/**
 * Elimina el archivo local de un episodio descargado.
 * - Parameter episodio: El episodio cuyo archivo se va a eliminar.
 */
func deleteLocalFile(for episodio: Episodio) {
    guard let fileURL = localFilePath(for: episodio) else { return }
    do {
        // Intenta borrar el archivo solo si existe.
        if FileManager.default.fileExists(atPath: fileURL.path) {
            try FileManager.default.removeItem(at: fileURL)
            print("Archivo eliminado: \(fileURL.path)")
        }
    } catch {
        print("Error al eliminar el archivo: \(error.localizedDescription)")
    }
}