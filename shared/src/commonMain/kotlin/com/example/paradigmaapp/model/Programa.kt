package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un programa de radio obtenido de la API de WordPress.
 * Corresponde a un término de una taxonomía personalizada 'radio' en WordPress.
 *
 * @property id El ID único del programa.
 * @property name El nombre del programa.
 * @property slug El slug amigable para URL del programa.
 * @property description La descripción del programa. Puede contener HTML y puede ser nulo.
 * @property count El número total de episodios (posts) asociados a este programa (término de taxonomía). Puede ser nulo.
 * @property imagenDelPrograma Objeto [ImagenProgramaInfo] que contiene la URL de la imagen del programa. Puede ser nulo.
 *
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Programa(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("count") val count: Int? = null,
    @SerialName("imagen_del_programa") val imagenDelPrograma: ImagenProgramaInfo? = null
) {
    /**
     * Proporciona una manera conveniente de acceder directamente a la URL de la imagen del programa.
     *
     * @return La URL de la imagen si [imagenDelPrograma] y su propiedad [ImagenProgramaInfo.guid] no son nulos;
     * devuelve null en caso contrario.
     */
    val imageUrl: String?
        get() = imagenDelPrograma?.guid
}


/**
 * Representa la información de la imagen destacada de un programa,
 * específicamente la URL directa (GUID) de la imagen.
 * Esta estructura anidada es común si la API devuelve la URL de la imagen dentro de un objeto.
 *
 * @property guid La URL (Global Unique Identifier) de la imagen del programa. Puede ser nulo.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class ImagenProgramaInfo(
    @SerialName("guid") val guid: String? = null
)
