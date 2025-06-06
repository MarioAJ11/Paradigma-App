package com.example.paradigmaapp.android.utils

import com.example.paradigmaapp.model.Programa
import kotlin.text.RegexOption

/**
 * Funciones de utilidad para el formateo y limpieza de cadenas de texto,
 * especialmente útil para el contenido proveniente de la API de WordPress.
 *
 * @author Mario Alguacil Juárez
 */

/**
 * Decodifica entidades HTML comunes en una cadena de texto.
 * Reemplaza secuencias como `&amp;`, `&lt;`, `&#039;` por sus caracteres correspondientes.
 *
 * @receiver La [String] que puede contener entidades HTML.
 * @return Una nueva [String] con las entidades HTML decodificadas.
 */
fun String.unescapeHtmlEntities(): String {
    return this.replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#039;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
        .replace("&#8217;", "’", ignoreCase = true)
        .replace("&#8211;", "–", ignoreCase = true)
        .replace("&#8212;", "—", ignoreCase = true)
        .replace("&#8230;", "…", ignoreCase = true)
        .replace("&#8220;", "“", ignoreCase = true)
        .replace("&#8221;", "”", ignoreCase = true)
        .replace("&#171;", "«", ignoreCase = true)
        .replace("&#187;", "»", ignoreCase = true)
        .replace("&nbsp;", " ", ignoreCase = true)
}

/**
 * Elimina todas las etiquetas HTML de una cadena de texto.
 *
 * @receiver La [String] que puede contener etiquetas HTML.
 * @return Una nueva [String] sin etiquetas HTML y con espacios en blanco extra eliminados.
 */
fun String.stripHtmlTags(): String {
    return this.replace(Regex("<[^>]*>"), "").trim()
}

/**
 * Extrae una descripción significativa y limpia de una cadena que puede contener HTML.
 * Se prioriza el contenido del primer párrafo (`<p>`). Si no existe, se limpia todo el HTML.
 *
 * @receiver La [String] original, que puede ser HTML.
 * @param maxLength La longitud máxima deseada para la descripción si no se encuentra un párrafo.
 * @return Una [String] limpia y, si es necesario, truncada.
 */
fun String.extractMeaningfulDescription(maxLength: Int = 350): String {
    val decodedHtml = this.unescapeHtmlEntities()
    val firstParagraphRegex = Regex("<p[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val paragraphMatch = firstParagraphRegex.find(decodedHtml)
    var meaningfulText: String

    if (paragraphMatch != null) {
        meaningfulText = paragraphMatch.groupValues[1].stripHtmlTags().trim()
    } else {
        meaningfulText = decodedHtml.stripHtmlTags().trim()
        if (meaningfulText.length > maxLength) {
            val trimPosition = meaningfulText.substring(0, maxLength.coerceAtMost(meaningfulText.length)).lastIndexOf(' ')
            meaningfulText = if (trimPosition > 0 && trimPosition > maxLength - 50) {
                meaningfulText.substring(0, trimPosition).trim() + "..."
            } else {
                meaningfulText.substring(0, maxLength.coerceAtMost(meaningfulText.length)).trim() + "..."
            }
        }
    }
    return meaningfulText.replace(Regex("\\s+"), " ").trim()
}

/**
 * Intenta extraer la URL de una imagen (`<img>`) del campo de descripción de un [Programa].
 *
 * @receiver El objeto [Programa] del cual se extraerá la URL.
 * @return La URL (String) de la primera etiqueta `<img>` encontrada, o `null`.
 */
fun Programa.imageUrlFromDescription(): String? {
    val description = this.description ?: return null
    val imgTagRegex = Regex("""<img[^>]+src\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
    return imgTagRegex.find(description)?.groups?.get(1)?.value
}