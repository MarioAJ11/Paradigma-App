package com.example.paradigmaapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Data class que representa la información del stream de radio obtenida de la API.
 * Utiliza [kotlinx.serialization.Serializable] para la deserialización automática de JSON.
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