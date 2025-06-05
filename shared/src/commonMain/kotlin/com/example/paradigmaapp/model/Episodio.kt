package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un episodio de podcast obtenido de la API de WordPress.
 * Corresponde a un Custom Post Type (CPT) en WordPress, usualmente 'posts' o un CPT específico para episodios.
 * Incluye campos para el título, contenido, metadatos y datos embebidos como imágenes destacadas y taxonomías.
 *
 * @property id El ID único del episodio.
 * @property renderedTitle El título del episodio, como un objeto [RenderedContent] que contiene el HTML renderizado.
 * @property renderedContent El contenido o descripción completa del episodio, como [RenderedContent]. Puede ser nulo.
 * @property renderedExcerpt El extracto o resumen del episodio, como [RenderedContent]. Puede ser nulo.
 * @property slug El slug amigable para URL del episodio.
 * @property date La fecha de publicación del episodio en formato GMT (ej. "2023-10-27T10:00:00").
 * @property meta Campos meta personalizados del episodio, como [MetaFields] que puede incluir el enlace de descarga.
 * @property embedded Datos embebidos relacionados con el episodio, como [Embedded] que puede incluir la imagen destacada y términos de taxonomía.
 * @property programaIds Lista de IDs de los programas (taxonomía 'radio') a los que pertenece este episodio. Puede ser nulo si no está asociado o la API no lo devuelve.
 * @property duration La duración del episodio (ej. "HH:MM:SS"). Este campo se inicializa con "--:--" y puede ser actualizado posteriormente.
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
    @SerialName("meta") val meta: MetaFields? = null,
    @SerialName("_embedded") val embedded: Embedded? = null,
    @SerialName("radio") val programaIds: List<Int>? = null,
    var duration: String = "--:--" // Duración formateada, puede no venir de la API directamente.
) {
    /**
     * Proporciona el título del episodio decodificando entidades HTML.
     */
    val title: String
        get() = renderedTitle.rendered.unescapeHtmlEntities()

    /**
     * Proporciona el contenido completo del episodio decodificando entidades HTML.
     * Devuelve null si `renderedContent` es nulo.
     */
    val content: String?
        get() = renderedContent?.rendered?.unescapeHtmlEntities()

    /**
     * Proporciona el extracto del episodio decodificando entidades HTML.
     * Devuelve null si `renderedExcerpt` es nulo.
     */
    val excerpt: String?
        get() = renderedExcerpt?.rendered?.unescapeHtmlEntities()

    /**
     * Proporciona la URL de descarga del archivo de audio del episodio.
     * Se obtiene del campo `enlaceDescarga` en `meta`. Devuelve null si no está disponible.
     */
    val archiveUrl: String?
        get() = meta?.enlaceDescarga

    /**
     * Proporciona la URL de la imagen destacada del episodio.
     * Intenta obtenerla de `sourceUrl` en `featuredMedia`. Si no está disponible,
     * busca en los tamaños alternativos dentro de `mediaDetails`.
     * Devuelve null si no se encuentra ninguna imagen.
     */
    val imageUrl: String?
        get() = embedded?.featuredMedia?.firstOrNull()?.sourceUrl
            ?: embedded?.featuredMedia?.firstOrNull()?.mediaDetails?.sizes?.values?.firstOrNull()?.sourceUrl

    /**
     * Decodifica entidades HTML comunes presentes en el contenido textual proveniente de WordPress.
     *
     * @receiver La cadena de texto que puede contener entidades HTML.
     * @return La cadena de texto con las entidades HTML reemplazadas por sus caracteres correspondientes.
     */
    private fun String.unescapeHtmlEntities(): String {
        return this.replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#039;", "'", ignoreCase = true)
            .replace("&apos;", "'", ignoreCase = true) // Añadido &apos; por si acaso
            .replace("&#8217;", "’", ignoreCase = true) // Comilla simple curva derecha
            .replace("&#8211;", "–", ignoreCase = true) // Guion corto (en dash)
            .replace("&#8212;", "—", ignoreCase = true) // Guion largo (em dash)
            .replace("&#8230;", "…", ignoreCase = true) // Puntos suspensivos
            .replace("&#8220;", "“", ignoreCase = true) // Comilla doble curva izquierda
            .replace("&#8221;", "”", ignoreCase = true) // Comilla doble curva derecha
            .replace("&#171;", "«", ignoreCase = true) // Comillas angulares izquierdas
            .replace("&#187;", "»", ignoreCase = true) // Comillas angulares derechas
            .replace("&nbsp;", " ", ignoreCase = true) // Espacio de no ruptura
    }
}

/**
 * Representa un campo de contenido (como título o cuerpo) que WordPress devuelve
 * con una propiedad "rendered" que contiene el HTML final.
 *
 * @property rendered El contenido HTML renderizado.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class RenderedContent(
    @SerialName("rendered") val rendered: String
)

/**
 * Contenedor para campos meta personalizados asociados a un [Episodio].
 *
 * @property enlaceDescarga La URL directa al archivo de audio del episodio,
 * típicamente alojado en servicios como archive.org. Puede ser nulo.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class MetaFields(
    @SerialName("enlacedescarga") val enlaceDescarga: String? = null
)

/**
 * Contenedor para datos embebidos (`_embedded`) que WordPress puede incluir en la respuesta de la API.
 * Esto evita la necesidad de hacer llamadas adicionales para obtener recursos relacionados.
 *
 * @property featuredMedia Lista de objetos [FeaturedMedia] que representan las imágenes destacadas.
 * Generalmente, solo habrá una o ninguna.
 * @property terms Lista de listas de términos de taxonomía ([Programa]) asociados al post.
 * La estructura es una lista de taxonomías, donde cada taxonomía es una lista de términos.
 * Por ejemplo, `wp:term: [[programa1, programa2], [categoria1]]`.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Embedded(
    @SerialName("wp:featuredmedia") val featuredMedia: List<FeaturedMedia>? = null,
    @SerialName("wp:term") val terms: List<List<Programa>>? = null
)

/**
 * Representa la información de un medio destacado (generalmente una imagen) de WordPress.
 *
 * @property id El ID único del medio en la biblioteca de WordPress.
 * @property sourceUrl La URL directa al archivo de imagen en su tamaño completo. Puede ser nulo.
 * @property mediaDetails Detalles adicionales del medio, como [MediaDetails] que incluye otros tamaños de imagen.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class FeaturedMedia(
    @SerialName("id") val id: Int,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("media_details") val mediaDetails: MediaDetails? = null
)

/**
 * Contiene detalles sobre un medio, específicamente los diferentes tamaños de imagen disponibles.
 *
 * @property sizes Un mapa donde la clave es el nombre del tamaño de la imagen (ej. "thumbnail", "medium")
 * y el valor es un objeto [ImageSize] con la URL a esa versión de la imagen. Puede ser nulo.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class MediaDetails(
    @SerialName("sizes") val sizes: Map<String, ImageSize>? = null
)

/**
 * Información sobre un tamaño específico de una imagen.
 *
 * @property sourceUrl La URL directa a este tamaño particular de la imagen.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class ImageSize(
    @SerialName("source_url") val sourceUrl: String
)