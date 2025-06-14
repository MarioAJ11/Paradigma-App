import Foundation

/**
 * Añade funcionalidades extra a la clase `String` de Swift.
 */
extension String {
    /**
     * Decodifica las entidades HTML más comunes en una cadena de texto.
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
     */
    func stripHtmlTags() -> String {
        return self.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression, range: nil).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /**
     * Extrae una descripción significativa y limpia de una cadena que puede contener HTML.
     */
    func extractMeaningfulDescription() -> String {
        let decodedHtml = self.unescaped()
        let firstParagraphRegex = try! NSRegularExpression(pattern: "<p[^>]*>(.*?)</p>", options: [.caseInsensitive, .dotMatchesLineSeparators])

        if let match = firstParagraphRegex.firstMatch(in: decodedHtml, options: [], range: NSRange(location: 0, length: decodedHtml.utf16.count)) {
            if let range = Range(match.range(at: 1), in: decodedHtml) {
                return String(decodedHtml[range]).stripHtmlTags()
            }
        }
        return decodedHtml.stripHtmlTags()
    }

    /**
     * Comprueba si una cadena no está vacía y no contiene únicamente espacios en blanco.
     * Es el equivalente a la función isNotBlank() de Kotlin.
     * Se define como una propiedad computada, por lo que se accede como `miString.isNotBlank`.
     */
    var isNotBlank: Bool {
        return !self.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
