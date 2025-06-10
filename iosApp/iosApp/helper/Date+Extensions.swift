import Foundation

extension String {
    /**
     * Formatea una fecha en formato ISO 8601 (como "2024-05-15T10:00:00")
     * a un formato más legible para el usuario en español (ej: "15 de mayo de 2024").
     * - Returns: La fecha formateada o una cadena vacía si el formato es inválido.
     */
    func formattedDate() -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime]

        guard let date = isoFormatter.date(from: self) else {
            // Intenta con un formato sin fracciones de segundo si el primero falla
            let fallbackFormatter = DateFormatter()
            fallbackFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
            fallbackFormatter.locale = Locale(identifier: "en_US_POSIX")
            fallbackFormatter.timeZone = TimeZone(secondsFromGMT: 0)
            guard let date = fallbackFormatter.date(from: self) else {
                return "Fecha desconocida"
            }
            return date.formatted(date: .long, time: .omitted)
        }

        // Define el formato de salida deseado (ej: "15 de mayo de 2024")
        let outputFormatter = DateFormatter()
        outputFormatter.dateStyle = .long
        outputFormatter.timeStyle = .none
        outputFormatter.locale = Locale(identifier: "es_ES")

        return outputFormatter.string(from: date)
    }
}