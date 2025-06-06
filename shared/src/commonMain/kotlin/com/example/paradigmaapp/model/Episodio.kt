package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un episodio de podcast obtenido de la API de WordPress.
 *
 * @property id El ID único del episodio.
 * @property renderedTitle El título del episodio, como un objeto [RenderedContent].
 * @property renderedContent El contenido o descripción completa del episodio, como [RenderedContent].
 * @property renderedExcerpt El extracto o resumen del episodio, como [RenderedContent].
 * @property slug El slug amigable para URL del episodio.
 * @property date La fecha de publicación del episodio en formato GMT.
 * @property meta Campos meta personalizados (estructura antigua para la URL).
 * @property urlDelPodcast La URL directa al archivo de audio del episodio (estructura nueva).
 * @property embedded Datos embebidos como la imagen destacada y términos de taxonomía.
 * @property programaIds Lista de IDs de los programas (taxonomía 'radio') a los que pertenece este episodio.
 * @property duration La duración del episodio.
 *
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Episodio(
    @SerialName("id") val id: Int,
    @SerialName("title") val renderedTitle: RenderedContent,
    @SerialName("content") val renderedContent: RenderedContent? = null,
    @SerialName("excerpt") val renderedExcerpt: RenderedContent? = null,
    @SerialName("slug") val slug: String,
    @SerialName("date_gmt") val date: String,
    @SerialName("meta") val meta: MetaFields? = null, // Se mantiene por retrocompatibilidad o por si tiene otros usos
    @SerialName("url_del_podcast") val urlDelPodcast: String? = null, // *** AÑADIDO EL NUEVO CAMPO ***
    @SerialName("_embedded") val embedded: Embedded? = null,
    @SerialName("radio") val programaIds: List<Int>? = null,
    var duration: String = "--:--"
) {
    /** Proporciona el título del episodio decodificando entidades HTML. */
    val title: String
        get() = renderedTitle.rendered.decodificarEntidadesHtml() // Renombrado para consistencia

    /** Proporciona el contenido completo del episodio decodificando entidades HTML. */
    val content: String?
        get() = renderedContent?.rendered?.decodificarEntidadesHtml()

    /** Proporciona el extracto del episodio decodificando entidades HTML. */
    val excerpt: String?
        get() = renderedExcerpt?.rendered?.decodificarEntidadesHtml()

    /**
     * Proporciona la URL de descarga del archivo de audio del episodio.
     * Prioriza el nuevo campo `urlDelPodcast`. Si es nulo, intenta usar el campo antiguo `meta.enlaceDescarga`
     * como respaldo para mantener la retrocompatibilidad.
     */
    val archiveUrl: String?
        get() = urlDelPodcast ?: meta?.enlaceDescarga // *** LÓGICA ACTUALIZADA ***

    /** Proporciona la URL de la imagen destacada del episodio. */
    val imageUrl: String?
        get() = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
            ?: embedded?.featuredMedia?.firstOrNull()?.mediaDetails?.sizes?.values?.firstOrNull()?.sourceUrl

    /** Decodifica entidades HTML comunes. */
    private fun String.decodificarEntidadesHtml(): String {
        return this.replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#039;", "'", ignoreCase = true)
            .replace("&apos;", "'", ignoreCase = true)
            .replace("&#8217;", "’", ignoreCase = true)
            .replace("&#8211;", "–", ignoreCase = true)
            .replace("&#8230;", "…", ignoreCase = true)
            .replace("&#8220;", "“", ignoreCase = true)
            .replace("&#8221;", "”", ignoreCase = true)
            .replace("&#171;", "«", ignoreCase = true)
            .replace("&#187;", "»", ignoreCase = true)
            .replace("&nbsp;", " ", ignoreCase = true)
    }
}

// --- Las demás data classes (RenderedContent, MetaFields, Embedded, etc.) se mantienen igual ---

/** Representa un campo de contenido (como título o cuerpo) renderizado por WordPress. */
@Serializable
data class RenderedContent(
    @SerialName("rendered") val rendered: String
)

/** Contenedor para campos meta personalizados (estructura antigua). */
@Serializable
data class MetaFields(
    @SerialName("enlacedescarga") val enlaceDescarga: String? = null
)

/** Contenedor para datos embebidos (`_embedded`). */
@Serializable
data class Embedded(
    @SerialName("wp:featuredmedia") val featuredMedia: List<FeaturedMedia>? = null,
    @SerialName("wp:term") val terms: List<List<Programa>>? = null
)

/** Representa la información de un medio destacado (imagen). */
@Serializable
data class FeaturedMedia(
    @SerialName("id") val id: Int,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("media_details") val mediaDetails: MediaDetails? = null
)

/** Contiene detalles sobre un medio, como otros tamaños de imagen. */
@Serializable
data class MediaDetails (
    @SerialName("sizes") val sizes: Map<String, ImageSize>? = null
)

/** Información sobre un tamaño específico de una imagen. */
@Serializable
data class ImageSize(
    @SerialName("source_url") val sourceUrl: String
)