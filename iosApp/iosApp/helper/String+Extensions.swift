import Foundation

/**
 * Añade funcionalidades extra a la clase `String` de Swift.
 */
extension String {
    /**
     * Decodifica las entidades HTML más comunes en una cadena de texto,
     * replicando la funcionalidad de la versión de Android.
     * - Returns: Una nueva cadena con las entidades reemplazadas por sus caracteres correspondientes.
     */
    func unescaped() -> String {
        var newString = self
        let entities = [
            "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": "\"", "&#039;": "'",
            "&apos;": "'", "&#8217;": "’", "&#8211;": "–", "&#8212;": "—",
            "&#8230;": "…", "&#8220;": "“", "&#8221;": "”", "&#171;": "«",
            "&#187;": "»", "&nbsp;": " "
        ]

        for (key, value) in entities {
            newString = newString.replacingOccurrences(of: key, with: value, options: .caseInsensitive)
        }
        return newString
    }

    /**
     * Elimina todas las etiquetas HTML de una cadena de texto.
     * - Returns: Una nueva `String` sin etiquetas HTML y con espacios en blanco extra eliminados.
     */
    func stripHtmlTags() -> String {
        return self.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression, range: nil).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /**
     * Extrae una descripción significativa y limpia de una cadena que puede contener HTML.
     * Intenta extraer el contenido del primer párrafo `<p>`. Si no lo encuentra, limpia todo el HTML.
     * - Returns: Una `String` limpia y formateada.
     */
    func extractMeaningfulDescription() -> String {
        let decodedHtml = self.unescaped()

        let firstParagraphRegex = try! NSRegularExpression(pattern: "<p[^>]*>(.*?)</p>", options: [.caseInsensitive, .dotMatchesLineSeparators])

        if let match = firstParagraphRegex.firstMatch(in: decodedHtml, options: [], range: NSRange(location: 0, length: decodedHtml.utf16.count)) {
            if let range = Range(match.range(at: 1), in: decodedHtml) {
                return String(decodedHtml[range]).stripHtmlTags()
            }
        }

        // Si no hay párrafos, limpia todo el string
        return decodedHtml.stripHtmlTags()
    }
}