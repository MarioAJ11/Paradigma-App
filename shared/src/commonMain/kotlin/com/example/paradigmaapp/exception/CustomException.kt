package com.example.paradigmaapp.exception

/**
 * Define excepciones personalizadas para la aplicación, permitiendo un manejo de errores
 * más granular y específico según el tipo de problema ocurrido (red, servidor, API).
 *
 * @author Mario Alguacil Juárez
 */

/**
 * Excepción lanzada cuando se detecta una ausencia de conexión a internet
 * o problemas que impiden la comunicación con la red.
 *
 * @param message Mensaje descriptivo del error. Por defecto, "No se pudo conectar. Revisa tu conexión a internet.".
 * @param cause La causa original de la excepción, útil para depuración. Opcional.
 */
class NoInternetException(
    override val message: String = "No se pudo conectar. Revisa tu conexión a internet.",
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Excepción lanzada para errores generales originados en el servidor
 * (ej. códigos de estado HTTP 5xx) u otros problemas inesperados del backend.
 *
 * @param userFriendlyMessage Mensaje amigable para mostrar al usuario.
 * Por defecto, "Ocurrió un error en el servidor. Inténtalo más tarde.".
 * @param cause La causa original de la excepción, para logging y depuración. Opcional.
 */
class ServerErrorException(
    val userFriendlyMessage: String = "Ocurrió un error en el servidor. Inténtalo más tarde.",
    override val cause: Throwable? = null
) : Exception(userFriendlyMessage, cause)

/**
 * Excepción lanzada para errores específicos devueltos por la API que no son necesariamente
 * errores de servidor (ej. recurso no encontrado 404, solicitud incorrecta 400).
 * Permite diferenciar errores de la lógica de la API de problemas de infraestructura del servidor.
 *
 * @param message Mensaje específico del error de la API.
 * @param cause La causa original de la excepción. Opcional.
 */
class ApiException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)