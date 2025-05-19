package com.example.paradigmaapp.android.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import kotlin.OptIn

/**
 * Data class que representa la información del stream de radio obtenida de la API.
 * Utiliza [kotlinx.serialization.Serializable] para la deserialización automática de JSON.
 *
 * @property title Título actual del stream o de la canción que se está reproduciendo. Puede ser nulo.
 * @property art URL de la imagen o carátula asociada a la reproducción actual. Puede ser nulo.
 * @property uniqueListeners Número de oyentes únicos conectados al stream. Puede ser nulo.
 * @property onlineListeners Número total de oyentes conectados al stream. Puede ser nulo.
 * @property bitrate Bitrate del stream de audio en kbps. Puede ser nulo.
 * @property djUsername Nombre del DJ o locutor actual. Puede ser nulo.
 * @property djProfile URL del perfil del DJ o locutor. Puede ser nulo.
 * @property history Lista de las últimas canciones o eventos reproducidos. Puede ser nulo.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(InternalSerializationApi::class)
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