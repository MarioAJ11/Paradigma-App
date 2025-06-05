package com.example.paradigmaapp.android.utils

import com.example.paradigmaapp.model.Programa
import kotlin.text.RegexOption

/**
 * Funciones de utilidad para el formateo y limpieza de cadenas de texto,
 * especialmente aquellas que contienen entidades HTML o etiquetas.
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
fun String.unescapeHtmlEntities(): String { //
    return this.replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#039;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true) // Alternativa para apóstrofo
        .replace("&#8217;", "’", ignoreCase = true) // Comilla simple curva derecha (apóstrofo tipográfico)
        .replace("&#8211;", "–", ignoreCase = true) // Guion corto (en dash)
        .replace("&#8212;", "—", ignoreCase = true) // Guion largo (em dash)
        .replace("&#8230;", "…", ignoreCase = true) // Puntos suspensivos
        .replace("&#8220;", "“", ignoreCase = true) // Comilla doble curva izquierda
        .replace("&#8221;", "”", ignoreCase = true) // Comilla doble curva derecha
        .replace("&#171;", "«", ignoreCase = true) // Comillas angulares francesas izquierdas
        .replace("&#187;", "»", ignoreCase = true) // Comillas angulares francesas derechas
        .replace("&nbsp;", " ", ignoreCase = true)   // Espacio de no ruptura
    // Añadir más reemplazos comunes si es necesario
}

/**
 * Elimina todas las etiquetas HTML de una cadena de texto.
 * Utiliza una expresión regular para encontrar y reemplazar cualquier secuencia
 * que comience con `<` y termine con `>`.
 *
 * @receiver La [String] que puede contener etiquetas HTML.
 * @return Una nueva [String] sin etiquetas HTML y con espacios en blanco extra al inicio/final eliminados.
 */
fun String.stripHtmlTags(): String {
    return this.replace(Regex("<[^>]*>"), "").trim()
}

/**
 * Extrae una descripción significativa y limpia de una cadena que puede contener HTML.
 * Prioriza el contenido del primer párrafo (`<p>`) si existe.
 * Si no hay párrafos, limpia todo el HTML y trunca el resultado si excede `maxLength`.
 *
 * @receiver La [String] original, que puede ser HTML.
 * @param maxLength La longitud máxima deseada para la descripción extraída si no se encuentra un párrafo.
 * Por defecto es 350 caracteres.
 * @return Una [String] limpia y, si es necesario, truncada, adecuada para mostrar como descripción.
 */
fun String.extractMeaningfulDescription(maxLength: Int = 350): String {
    // 1. Decodificar entidades HTML una sola vez al principio.
    val decodedHtml = this.unescapeHtmlEntities() //

    // 2. Intentar extraer el contenido del primer tag <p> (ignorando mayúsculas/minúsculas y permitiendo atributos).
    val firstParagraphRegex = Regex(
        pattern = "<p[^>]*>(.*?)</p>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ) //
    val paragraphMatch = firstParagraphRegex.find(decodedHtml)
    var meaningfulText: String

    if (paragraphMatch != null) {
        // Si se encuentra un párrafo, se toma su contenido y se limpia cualquier HTML interno residual.
        meaningfulText = paragraphMatch.groupValues[1].stripHtmlTags().trim() //
        // Consolidar múltiples saltos de línea/espacios a un solo espacio o un solo salto de línea.
        meaningfulText = meaningfulText.replace(Regex("\\s*\\n\\s*"), "\n").trim()
        meaningfulText = meaningfulText.replace(Regex("\\s+"), " ") // Consolida espacios múltiples
    } else {
        // 3. Si no hay tag <p> explícito, limpiar todo el HTML del string decodificado.
        meaningfulText = decodedHtml.stripHtmlTags().trim()
        meaningfulText = meaningfulText.replace(Regex("\\s*\\n\\s*"), "\n").trim()
        meaningfulText = meaningfulText.replace(Regex("\\s+"), " ")

        // Truncar si es demasiado largo Y no se extrajo de un <p> específico (para evitar truncar párrafos cortos).
        if (meaningfulText.length > maxLength) {
            // Intentar truncar en un espacio para no cortar palabras.
            val trimPosition = meaningfulText.substring(0, maxLength.coerceAtMost(meaningfulText.length)).lastIndexOf(' ')
            meaningfulText = if (trimPosition > 0 && trimPosition > maxLength - 50) { // Evitar truncar a una palabra muy corta al final
                meaningfulText.substring(0, trimPosition).trim() + "..."
            } else {
                // Si no se encuentra un espacio adecuado o resultaría en una palabra muy corta, truncar directamente.
                meaningfulText.substring(0, maxLength.coerceAtMost(meaningfulText.length)).trim() + "..."
            }
        }
    }
    return meaningfulText.trim() // Asegurar que no haya espacios extra al final.
}

/**
 * Intenta extraer la URL de una imagen (`<img>`) del campo de descripción de un [Programa].
 * Útil si la imagen del programa está incrustada como HTML en su descripción.
 *
 * @receiver El objeto [Programa] del cual se extraerá la URL.
 * @return La URL (String) de la primera etiqueta `<img>` encontrada en la descripción,
 * o `null` si no hay descripción o no se encuentra ninguna etiqueta `<img>` con `src`.
 */
fun Programa.imageUrlFromDescription(): String? { //
    val desc = this.description ?: return null // Si no hay descripción, no hay nada que hacer.
    // Expresión regular para encontrar la URL en el atributo src de una etiqueta img.
    val imgTagRegex = Regex("""<img[^>]+src\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
    return imgTagRegex.find(desc)?.groups?.get(1)?.value
}