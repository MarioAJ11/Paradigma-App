package com.example.paradigmaapp.android.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import kotlin.OptIn

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