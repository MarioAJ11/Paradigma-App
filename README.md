# Paradigma App

## Descripción del Proyecto

Paradigma App es una aplicación móvil desarrollada con Kotlin Multiplatform (KMP) diseñada para ofrecer a los usuarios una experiencia auditiva completa del contenido de Paradigma Media. La aplicación permite a los usuarios escuchar programas de radio, episodios de podcasts bajo demanda y la transmisión en vivo de Andaina FM.

La arquitectura del proyecto se basa en una lógica de negocio compartida en Kotlin, que gestiona el acceso a la red, la caché local y los modelos de datos, asegurando la consistencia entre las plataformas Android e iOS.

**Características Principales:**

* **Kotlin Multiplatform (KMP)**: La lógica de negocio principal, incluyendo el acceso a la red (Ktor) y los modelos de datos (kotlinx.serialization), está escrita en Kotlin y compartida entre Android e iOS.
* **Arquitectura Offline-First con Caché Local**: La aplicación utiliza una base de datos local (SQLDelight) para cachear el contenido de programas y episodios. Esto permite una navegación por el contenido prácticamente instantánea después de la primera carga y asegura la funcionalidad de la app incluso sin conexión a internet.
* **Interfaz Nativa**:
    * **Android**: Interfaz de usuario moderna y reactiva construida con Jetpack Compose.
    * **iOS**: Interfaz de usuario construida con SwiftUI (actualmente en fase de desarrollo inicial).
* **Carga Eficiente con Paginación**: Las listas largas de episodios se cargan de forma progresiva a medida que el usuario se desplaza (`Infinite Scroll`), optimizando el rendimiento y el consumo de datos gracias a la librería Jetpack Paging 3.
* **Funcionalidades del Reproductor y Contenido**:
    * Reproducción de episodios de podcasts (online y desde la caché).
    * Streaming en vivo de Andaina FM con obtención de metadatos.
    * Descarga de episodios para escucha 100% offline.
    * Gestión de cola de reproducción y seguimiento del progreso de escucha.
* **Descubrimiento de Contenido**:
    * Navegación por programas y sus listas de episodios cacheadas.
    * Funcionalidad de búsqueda para encontrar episodios específicos.

El proyecto utiliza tecnologías modernas como Ktor para networking, SQLDelight para la base de datos local, ExoPlayer (Media3) para la reproducción de audio en Android y Coroutines para la programación asíncrona.

## Información sobre Despliegue

El despliegue de Paradigma App involucra la compilación de las aplicaciones nativas y la correcta configuración de sus dependencias.

* **Android**:
    1.  Clonar el repositorio y abrir el proyecto en Android Studio.
    2.  Asegurar que el `applicationId` en `androidApp/build.gradle.kts` sea único.
    3.  Para generar una versión de lanzamiento, es **imprescindible** crear un Keystore (almacén de claves) y configurar el bloque `signingConfigs` en `androidApp/build.gradle.kts` para firmar la aplicación.
    4.  Generar un Android App Bundle (`.aab`) firmado para subir a Google Play.
* **iOS**:
    1.  Abrir el proyecto `iosApp/iosApp.xcodeproj` en Xcode sobre un entorno macOS.
    2.  Configurar el equipo de desarrollo y el provisioning profile en Xcode.
    3.  Compilar y ejecutar en un simulador o dispositivo físico.
* **Backend y Base de Datos**:
    * La aplicación depende de una API REST de WordPress que debe estar en línea.
    * **Importante**: Si en el futuro se modifica la estructura de las tablas de la base de datos (los archivos `.sq`), será necesario implementar una **migración** en SQLDelight para actualizar la base de datos de los usuarios existentes sin que pierdan sus datos. Para el desarrollo, basta con desinstalar y reinstalar la app.

Para instrucciones más detalladas, consulta el [Manual de Despliegue](wiki/Manual_Despliegue.md) en la Wiki del proyecto.

## Información sobre cómo usarlo

Paradigma App está diseñada para ser intuitiva y robusta.

1.  **Navegación Rápida y Offline**: Gracias a la caché local, una vez que hayas explorado los programas o los episodios de un programa, podrás volver a verlos al instante, incluso sin conexión a internet.
2.  **Descubrimiento**: Usa la barra de navegación inferior para acceder a las diferentes secciones: Buscar, Seguir Escuchando, Descargas, Cola y Ajustes.
3.  **Reproducción**:
    * Toca un episodio para empezar a escucharlo (requiere internet si no está descargado).
    * El reproductor de audio aparecerá en la parte inferior, permitiendo controlar la reproducción.
    * Puedes alternar en cualquier momento a la radio en directo de Andaina FM.
4.  **Descargas para Escucha Offline**: Para escuchar un episodio sin conexión, utiliza la opción "Descargar" del menú del episodio. Podrás encontrar todos tus episodios descargados en la sección "Descargas".

Para una guía completa sobre todas las funcionalidades, consulta el [Manual de Usuario](wiki/Manual_Usuario.md) en la Wiki del proyecto.

## Autor

* **Mario Alguacil Juárez** - Desarrollador principal.
    * GitHub: [MarioAJ11](https://github.com/MarioAJ11)

---