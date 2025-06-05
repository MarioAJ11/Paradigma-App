package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa la información del stream de radio en directo, obtenida de una API específica
 * Utiliza [kotlinx.serialization.Serializable] para la deserialización automática desde JSON.
 *
 * @property title El título de la canción o programa actualmente en emisión. Puede ser nulo.
 * @property art La URL a la carátula o imagen asociada con la emisión actual. Puede ser nulo.
 * @property uniqueListeners Número de oyentes únicos (histórico o en un periodo). Puede ser nulo.
 * @property onlineListeners Número de oyentes conectados actualmente. Puede ser nulo.
 * @property bitrate La tasa de bits del stream (ej. 128 kbps). Puede ser nulo.
 * @property djUsername El nombre de usuario o identificador del DJ/locutor actual. Puede ser nulo.
 * @property djProfile Enlace al perfil del DJ/locutor o información adicional. Puede ser nulo.
 * @property history Lista de títulos de canciones o programas emitidos recientemente. Puede ser nulo.
 *
 * @author Mario Alguacil Juárez
 */
@Serializable
data class RadioInfo(
    @SerialName("title") val title: String?,
    @SerialName("art") val art: String?,
    @SerialName("ulistener") val uniqueListeners: Int?,
    @SerialName("listeners") val onlineListeners: Int?,
    @SerialName("bitrate") val bitrate: Int?,
    @SerialName("djusername") val djUsername: String?,
    @SerialName("djprofile") val djProfile: String?,
    @SerialName("history") val history: List<String>?
)