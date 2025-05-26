package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un episodio (podcast) obtenido de la API de WordPress.
 * Corresponde a un custom post type 'episodios'.
 *
 * @property id El ID único del episodio.
 * @property renderedTitle El objeto que contiene el título renderizado del episodio.
 * @property renderedContent El objeto que contiene el contenido o descripción renderizada del episodio.
 * @property renderedExcerpt El objeto que contiene el extracto renderizado del episodio.
 * @property slug El slug del episodio.
 * @property date La fecha de publicación del episodio en formato GMT.
 * @property meta Campos meta personalizados, incluyendo el enlace de descarga.
 * @property embedded Datos embebidos como la imagen destacada y términos de taxonomía.
 * @property programaIds Lista de IDs de los programas (taxonomía 'radio') a los que pertenece este episodio.
 * @property duration La duración del episodio (ej. "HH:MM:SS"). Actualmente no se obtiene de la API.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Episodio(
    @SerialName("id") val id: Int,
    @SerialName("title") val renderedTitle: RenderedContent,
    @SerialName("content") val renderedContent: RenderedContent? = null,
    @SerialName("excerpt") val renderedExcerpt: RenderedContent? = null,
    @SerialName("slug") val slug: String,
    @SerialName("date_gmt") val date: String, // Fecha en GMT
    @SerialName("meta") val meta: MetaFields? = null,
    @SerialName("_embedded") val embedded: Embedded? = null,
    @SerialName("radio") val programaIds: List<Int>? = null, // IDs de la taxonomía 'radio'
    var duration: String = "--:--" // Duración formateada, no viene de la API de pruebas
) {
    val title: String
        get() = renderedTitle.rendered.unescapeHtmlEntities()

    val content: String?
        get() = renderedContent?.rendered?.unescapeHtmlEntities()

    val excerpt: String?
        get() = renderedExcerpt?.rendered?.unescapeHtmlEntities()

    val archiveUrl: String?
        get() = meta?.enlaceDescarga

    val imageUrl: String?
        get() = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
            ?: embedded?.featuredMedia?.firstOrNull()?.mediaDetails?.sizes?.values?.firstOrNull()?.sourceUrl

    // Función simple para decodificar entidades HTML comunes en títulos y contenido
    private fun String.unescapeHtmlEntities(): String {
        return this.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#8217;", "’")
            .replace("&#8211;", "–")
            .replace("&#8230;", "…")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#171;", "«")
            .replace("&#187;", "»")
    }
}

/**
 * Representa un campo de contenido renderizado por WordPress (ej. título, contenido).
 */
@Serializable
data class RenderedContent(
    @SerialName("rendered") val rendered: String
)

/**
 * Contenedor para campos meta personalizados.
 *
 * @property enlaceDescarga El enlace al archivo de audio en archive.org.
 */
@Serializable
data class MetaFields(
    @SerialName("enlacedescarga") val enlaceDescarga: String? = null
)

/**
 * Contenedor para datos embebidos de WordPress (`_embedded`).
 *
 * @property featuredMedia Lista de medios destacados (imágenes).
 * @property terms Lista de términos de taxonomía asociados.
 */
@Serializable
data class Embedded(
    @SerialName("wp:featuredmedia") val featuredMedia: List<FeaturedMedia>? = null,
    @SerialName("wp:term") val terms: List<List<Programa>>? = null // Para obtener los términos de taxonomía directamente
)

/**
 * Representa la información de un medio destacado (imagen).
 *
 * @property id El ID del medio.
 * @property sourceUrl La URL directa al archivo de imagen a tamaño completo.
 * @property mediaDetails Detalles adicionales del medio, como otros tamaños de imagen.
 */
@Serializable
data class FeaturedMedia(
    @SerialName("id") val id: Int,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("media_details") val mediaDetails: MediaDetails? = null
)

/**
 * Detalles del medio, puede incluir tamaños de imagen.
 *
 * @property sizes Un mapa de los diferentes tamaños de imagen disponibles y sus URLs.
 */
@Serializable
data class MediaDetails (
    @SerialName("sizes") val sizes: Map<String, ImageSize>? = null
)

/**
 * Información sobre un tamaño específico de imagen.
 *
 * @property sourceUrl La URL a este tamaño específico de la imagen.
 */
@Serializable
data class ImageSize(
    @SerialName("source_url") val sourceUrl: String
)