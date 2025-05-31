package com.example.paradigmaapp

import platform.UIKit.UIDevice

/**
 * Mi implementación `actual` de la interfaz [Platform] para iOS.
 * Utilizo [UIDevice] del framework UIKit de iOS para obtener el nombre
 * del sistema operativo y su versión actual.
 *
 * @author Mario Alguacil Juárez
 */
class IOSPlatform: Platform { // La clase IOSPlatform implementa la interfaz común Platform.
    // La propiedad 'name' se construye concatenando el nombre del sistema y su versión.
    // Ejemplo: "iOS 15.4".
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Mi implementación `actual` de la función [getPlatform] para iOS.
 * Devuelve una nueva instancia de [IOSPlatform], proporcionando así
 * la implementación específica de la plataforma para el código común.
 *
 * @author Mario Alguacil Juárez
 */
actual fun getPlatform(): Platform = IOSPlatform()