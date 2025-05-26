package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa un programa de radio obtenido de la API de WordPress.
 * Corresponde a un término de la taxonomía 'radio'.
 *
 * @property id El ID único del programa.
 * @property name El nombre del programa.
 * @property slug El slug del programa.
 * @property description La descripción del programa (si está disponible).
 * @property count El número de episodios asociados a este programa.
 * @author Mario Alguacil Juárez
 */
@Serializable
data class Programa(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("count") val count: Int? = null
)