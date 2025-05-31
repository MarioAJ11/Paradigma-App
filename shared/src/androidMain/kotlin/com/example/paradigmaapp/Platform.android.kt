package com.example.paradigmaapp

import android.os.Build

/**
 * Mi implementación `actual` de la interfaz [Platform] para Android.
 * Proporciona el nombre del sistema operativo Android junto con su nivel de API.
 *
 * @author Mario Alguacil Juárez
 */
class AndroidPlatform : Platform {
    // La propiedad 'name' devuelve una cadena como "Android 30".
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Mi implementación `actual` de la función [getPlatform] para Android.
 * Devuelve una instancia de [AndroidPlatform].
 *
 * @author Mario Alguacil Juárez
 */
actual fun getPlatform(): Platform = AndroidPlatform()