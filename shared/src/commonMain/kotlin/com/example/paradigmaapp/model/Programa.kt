package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa la información de la imagen del programa obtenida de la API.
 * Específicamente, nos interesa la URL directa de la imagen.
 */
@Serializable
data class ImagenProgramaInfo(
    // El campo 'guid' en el JSON del objeto 'imagen_del_programa' contiene la URL.
    @SerialName("guid") val guid: String? = null
)

/**
 * Representa un programa de radio obtenido de la API de WordPress.
 * Corresponde a un término de la taxonomía 'radio'.
 *
 * @property id El ID único del programa.
 * @property name El nombre del programa.
 * @property slug El slug del programa.
 * @property description La descripción del programa (si está disponible).
 * @property count El número de episodios asociados a este programa.
 * @property imagenDelPrograma Objeto que contiene la información de la imagen del programa.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Programa(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("count") val count: Int? = null,
    // Este campo mapea el objeto JSON "imagen_del_programa"
    @SerialName("imagen_del_programa") val imagenDelPrograma: ImagenProgramaInfo? = null
) {
    // Getter conveniente para acceder fácilmente a la URL de la imagen.
    // Si 'imagenDelPrograma' o su 'guid' son nulos, esto devolverá null.
    val imageUrl: String?
        get() = imagenDelPrograma?.guid
}