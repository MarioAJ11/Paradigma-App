package com.example.paradigmaapp.android.utils

import com.example.paradigmaapp.model.Programa
import kotlin.text.Regex
import kotlin.text.RegexOption

fun String.unescapeHtmlEntities(): String {
    return this.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&#8217;", "’")
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&#8230;", "…")
        .replace("&#8220;", "“")
        .replace("&#8221;", "”")
        .replace("&#171;", "«")
        .replace("&#187;", "»")
        .replace("&nbsp;", " ")
}

fun String.stripHtmlTags(): String {
    return this.replace(Regex("<[^>]*>"), "").trim()
}

fun String.extractMeaningfulDescription(maxLength: Int = 350): String {
    // 1. Decodificar entidades HTML una sola vez al principio.
    val decodedHtml = this.unescapeHtmlEntities()

    // 2. Intentar extraer el contenido del primer tag <p>
    val firstParagraphRegex = Regex(
        pattern = "<p[^>]*>(.*?)</p>", // Permite atributos en el tag <p>
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val paragraphMatch = firstParagraphRegex.find(decodedHtml) // Buscar una sola vez
    var meaningfulText: String
    val extractedFromActualParagraph = paragraphMatch != null // Guardar si se encontró un <p>

    if (extractedFromActualParagraph) {
        // Si encontramos un párrafo, tomamos su contenido y limpiamos cualquier HTML interno
        meaningfulText = paragraphMatch!!.groupValues[1].stripHtmlTags().trim() // Usar !! porque ya comprobamos != null
        // Reemplazar múltiples saltos de línea/espacios que puedan quedar tras stripHtmlTags a un solo espacio o \n
        meaningfulText = meaningfulText.replace(Regex("\\s*\\n\\s*"), "\n").trim() // Consolida saltos de línea
        meaningfulText = meaningfulText.replace(Regex("\\s+"), " ") // Consolida espacios múltiples a uno solo
    } else {
        // 3. Si no hay tag <p> explícito, limpiar todo el HTML del string decodificado
        meaningfulText = decodedHtml.stripHtmlTags().trim()
        meaningfulText = meaningfulText.replace(Regex("\\s*\\n\\s*"), "\n").trim()
        meaningfulText = meaningfulText.replace(Regex("\\s+"), " ")

        // Truncar si es demasiado largo Y no vino de un <p> específico
        if (meaningfulText.length > maxLength) {
            val trimPosition = meaningfulText.substring(0, maxLength).lastIndexOf(' ')
            meaningfulText = if (trimPosition > 0 && trimPosition > maxLength - 50) { // Evitar truncar a una palabra muy corta
                meaningfulText.substring(0, trimPosition).trim() + "..."
            } else {
                meaningfulText.substring(0, maxLength).trim() + "..."
            }
        }
    }
    return meaningfulText
}

fun Programa.imageUrlFromDescription(): String? {
    val desc = this.description ?: return null
    val imgTagRegex = Regex("""<img[^>]+src\s*=\s*['"]([^'"]+)['"]""")
    return imgTagRegex.find(desc)?.groups?.get(1)?.value
}