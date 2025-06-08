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
            "&amp;": "&",
            "&lt;": "<",
            "&gt;": ">",
            "&quot;": "\"",
            "&#039;": "'",
            "&apos;": "'",
            "&#8217;": "’",
            "&#8211;": "–",
            "&#8230;": "…",
            "&#8220;": "“",
            "&#8221;": "”",
            "&nbsp;": " "
        ]

        for (key, value) in entities {
            newString = newString.replacingOccurrences(of: key, with: value)
        }
        return newString
    }
}