package com.example.paradigmaapp.exception

/**
 * Excepción lanzada cuando no hay conexión a internet detectada.
 */
class NoInternetException(message: String = "No se pudo conectar. Revisa tu conexión a internet.") : Exception(message)

/**
 * Excepción lanzada para errores generales del servidor.
 * @param userFriendlyMessage Mensaje para mostrar al usuario.
 * @param cause La causa original de la excepción, para logging.
 */
class ServerErrorException(val userFriendlyMessage: String = "Ocurrió un error en el servidor. Inténtalo más tarde.", cause: Throwable? = null) : Exception(userFriendlyMessage, cause)

/**
 * Excepción (opcional, para diferenciar de ServerErrorException) si la API devuelve un error conocido.
 * @param message Mensaje específico del error de API.
 */
class ApiException(message: String) : Exception(message)