# Módulo Proyecto Integrado del IES Gran Capitán

## Descripción del Proyecto

Paradigma App es una aplicación móvil desarrollada con Kotlin Multiplatform (KMP) que ofrece a los usuarios una experiencia auditiva completa del contenido de Paradigma Media. Permite escuchar programas de radio, episodios de podcasts bajo demanda y la transmisión en vivo de Andaina FM. La arquitectura se centra en una lógica de negocio compartida en Kotlin, asegurando consistencia y eficiencia entre las plataformas Android (Jetpack Compose) e iOS (SwiftUI).

Para una explicación detallada de la justificación, los objetivos y la arquitectura, consulta la **[Documentación del Proyecto en la Wiki](https://github.com/marioaj11/paradigma-app/wiki/1.-Introducción-y-Justificación)**.

## Información sobre Despliegue

El despliegue de la aplicación requiere la compilación de los módulos nativos (`androidApp`, `iosApp`) y la configuración de sus dependencias y firmas de código.

* **Android**: Es imprescindible generar un Keystore y configurar el archivo `androidApp/build.gradle.kts` para firmar el App Bundle (`.aab`) de lanzamiento.
* **iOS**: Se debe configurar un equipo de desarrollo y un provisioning profile en Xcode.

Para ver los pasos detallados, consulta el **[Manual de Despliegue en la Wiki](https://github.com/marioaj11/paradigma-app/wiki/6.-Manual-de-Despliegue)**.

## Información sobre cómo usarlo

Paradigma App está diseñada para ser intuitiva. Gracias a la caché local, la navegación por programas y episodios es casi instantánea después de la primera carga.

1.  **Explora**: Usa la barra de navegación inferior para descubrir contenido.
2.  **Reproduce**: Toca cualquier episodio para iniciar la reproducción. Un reproductor compacto aparecerá en la parte inferior.
3.  **Escucha Offline**: Descarga episodios para escucharlos sin conexión a internet desde la sección "Descargas".

Para una guía completa con todas las funcionalidades, consulta el **[Manual de Usuario en la Wiki](https://github.com/marioaj11/paradigma-app/wiki/7.-Manual-de-Usuario)**.

## Autor

* **Mario Alguacil Juárez** - Desarrollador principal.
  * GitHub: [MarioAJ11](https://github.com/MarioAJ11)