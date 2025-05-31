package com.example.paradigmaapp

/**
 * Defino una interfaz común [Platform] para obtener información específica de la plataforma,
 * como su nombre. Esto me permite tener código en `commonMain` que puede mostrar, por ejemplo,
 * "Android X.Y" o "iOS Z.W" sin conocer los detalles de implementación de cada plataforma.
 *
 * @author Mario Alguacil Juárez
 */
interface Platform {
    val name: String // Propiedad que cada plataforma implementará para devolver su nombre y versión.
}

/**
 * Declaro una función `expect` para obtener una instancia de [Platform]
 * específica para la plataforma actual. La implementación real (`actual`)
 * se encuentra en los módulos de cada plataforma (`androidMain`, `iosMain`).
 *
 * @author Mario Alguacil Juárez
 */
expect fun getPlatform(): Platform