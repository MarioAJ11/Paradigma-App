# Paradigma App

## Descripción del Proyecto

Paradigma App es una aplicación móvil desarrollada con Kotlin Multiplatform (KMP) diseñada para ofrecer a los usuarios una experiencia auditiva completa del contenido de Paradigma Media. La aplicación permite a los usuarios escuchar programas de radio, episodios de podcasts bajo demanda y la transmisión en vivo de Andaina FM.

**Características Principales:**

* **Kotlin Multiplatform (KMP)**: La lógica de negocio principal, incluyendo el acceso a la red y los modelos de datos, está escrita en Kotlin y compartida entre las plataformas Android e iOS, optimizando el desarrollo y la consistencia.
* **Interfaz Nativa**:
    * **Android**: Interfaz de usuario moderna y reactiva construida con Jetpack Compose.
    * **iOS**: Interfaz de usuario construida con SwiftUI (actualmente en una fase inicial de desarrollo).
* **Contenido de WordPress**: Integración con un backend de WordPress que sirve como API para listar programas y sus respectivos episodios, incluyendo metadatos como títulos, descripciones, imágenes y enlaces de descarga.
* **Streaming en Vivo**: Reproducción de la transmisión en directo de Andaina FM.
* **Funcionalidades del Reproductor**:
    * Reproducción de episodios de podcasts (online y descargados).
    * Controles de reproducción estándar (play, pausa, barra de progreso).
    * Gestión de cola de reproducción.
    * Descarga de episodios para escucha offline.
    * Seguimiento del progreso de escucha de los episodios.
* **Descubrimiento de Contenido**:
    * Navegación por programas y sus episodios.
    * Funcionalidad de búsqueda para encontrar episodios específicos.
* **Personalización y Gestión**:
    * Pantalla de "Seguir Escuchando" para retomar episodios no finalizados.
    * Pantalla de "Descargas" para gestionar el contenido offline.
    * Ajustes básicos de la aplicación, como la activación/desactivación del streaming por defecto.

El proyecto utiliza tecnologías como Ktor para networking, kotlinx.serialization para el manejo de JSON, ExoPlayer (Media3) para la reproducción de audio en Android, y Coroutines para la programación asíncrona.

## Información sobre Despliegue

El despliegue de Paradigma App involucra la compilación de las aplicaciones nativas para Android e iOS y asegurar la correcta configuración y accesibilidad del backend de WordPress y el stream de Andaina.

* **Android**:
    1.  Clonar el repositorio.
    2.  Abrir el proyecto en Android Studio.
    3.  Sincronizar el proyecto con los archivos Gradle.
    4.  Ejecutar la aplicación en un emulador o dispositivo Android físico.
    5.  Para generar una versión de lanzamiento, configurar las claves de firma y generar un APK o App Bundle firmado.
* **iOS**:
    1.  Clonar el repositorio.
    2.  Navegar al directorio `iosApp`.
    3.  Abrir el proyecto `.xcodeproj` o `.xcworkspace` en Xcode.
    4.  Configurar el equipo de desarrollo y el provisioning profile.
    5.  Compilar y ejecutar en un simulador de iOS o dispositivo físico.
* **Backend**:
    * Asegurar que el sitio WordPress configurado como backend esté en línea y accesible.
    * Verificar que el plugin de WordPress que gestiona los "episodios" (Custom Post Type) y "programas" (taxonomía "radio") esté activo y configurado correctamente.
    * La API REST de WordPress debe estar habilitada y ser accesible para la aplicación.
    * Las URLs del stream de Andaina FM y su API de metadatos deben estar operativas.

Para instrucciones más detalladas, consulta el [Manual de Despliegue](wiki/Manual_Despliegue.md) en la Wiki del proyecto.

## Información sobre cómo usarlo

Paradigma App está diseñada para ser intuitiva, permitiendo un fácil acceso a todo el contenido de Paradigma Media.

1.  **Pantalla de Inicio**: Muestra los programas disponibles.
2.  **Navegación**: Utiliza la barra de navegación inferior para acceder a:
    * **Buscar**: Encuentra episodios por título.
    * **Continuar**: Reanuda la escucha de episodios que dejaste a medias.
    * **Descargas**: Accede a los episodios que has descargado para escucharlos sin conexión.
    * **Cola**: Gestiona tu lista de episodios por escuchar.
    * **Ajustes**: Configura opciones de la aplicación, como el comportamiento del streaming.
3.  **Explorar Programas**: Toca un programa en la pantalla de inicio para ver la lista de sus episodios.
4.  **Reproducción**:
    * Toca un episodio para empezar a reproducirlo.
    * El reproductor de audio aparecerá en la parte inferior de la pantalla, mostrando la información del episodio actual (o del stream en vivo) y los controles de reproducción (play/pausa, progreso).
    * Puedes cambiar entre la reproducción de podcasts y el stream en vivo de Andaina FM.
5.  **Acciones de Episodio**: En las listas de episodios, puedes realizar acciones como añadir a la cola, descargar, o eliminar descargas a través de un menú contextual (generalmente con una pulsación larga o un icono de tres puntos).

Para una guía completa sobre todas las funcionalidades, consulta el [Manual de Usuario](wiki/Manual_Usuario.md) en la Wiki del proyecto.

## Autor

* **Mario Alguacil Juárez** - Desarrollador principal.
    * GitHub: [MarioAJ11](https://github.com/MarioAJ11)

---