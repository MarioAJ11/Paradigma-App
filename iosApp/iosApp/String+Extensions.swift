import Foundation

// Creamos una extensión para la clase String de Swift
extension String {
    // Función que decodifica las entidades HTML más comunes
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