package com.example.paradigmaapp.android.utils

import com.example.paradigmaapp.model.Programa // Necesario si imageUrlFromDescription es extensión de Programa

/**
 * Decodifica entidades HTML comunes de una cadena de texto.
 * @receiver La cadena de texto que puede contener entidades HTML.
 * @return La cadena con las entidades HTML reemplazadas por sus caracteres correspondientes.
 * @author Mario Alguacil Juárez
 */
fun String.unescapeHtmlEntities(): String {
    return this.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'") // Otra forma común para apóstrofo
        .replace("&#8217;", "’") // Apóstrofo rizado derecho
        .replace("&#8211;", "–") // Guion corto (en dash)
        .replace("&#8212;", "—") // Guion largo (em dash)
        .replace("&#8230;", "…") // Puntos suspensivos
        .replace("&#8220;", "“") // Comilla doble izquierda
        .replace("&#8221;", "”") // Comilla doble derecha
        .replace("&#171;", "«")  // Comillas angulares izquierdas (laquo)
        .replace("&#187;", "»")  // Comillas angulares derechas (raquo)
        .replace("&nbsp;", " ") // Espacio de no ruptura
    // En caso necesario de necesario añadir más se podría
}

/**
 * Elimina etiquetas HTML de una cadena de texto de forma básica.
 * Para una solución más robusta, considera usar una biblioteca de parseo HTML si el HTML es complejo.
 * @receiver La cadena de texto que puede contener etiquetas HTML.
 * @return La cadena sin etiquetas HTML, con espacios en blanco al inicio/final eliminados.
 * @author Mario Alguacil Juárez
 */
fun String.extractTextFromHtml(): String {
    // Regex para eliminar cualquier cosa entre < y >
    return this.replace(Regex("<[^>]*>"), "").trim()
}

/**
 * Intenta extraer una URL de imagen de la descripción de un programa.
 * Este es un método heurístico y puede necesitar ajustes basados en cómo tu WordPress
 * almacena o muestra las imágenes de los programas en sus descripciones.
 * Idealmente, la API de WordPress proveería un campo dedicado para la imagen del programa.
 *
 * @receiver El objeto [Programa] del cual se intenta extraer la URL de la imagen.
 * @return La URL de la imagen encontrada, o null si no se encuentra ninguna.
 * @author Mario Alguacil Juárez
 */
fun Programa.imageUrlFromDescription(): String? {
    val desc = this.description ?: return null // Si no hay descripción, no hay imagen que extraer.
    // Regex simple para buscar la primera etiqueta <img> y extraer su atributo src.
    val imgTagRegex = Regex("""<img[^>]+src\s*=\s*['"]([^'"]+)['"]""")
    return imgTagRegex.find(desc)?.groups?.get(1)?.value
}