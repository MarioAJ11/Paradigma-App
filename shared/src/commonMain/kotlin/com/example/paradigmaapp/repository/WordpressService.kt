package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import com.example.paradigmaapp.repository.contracts.ProgramaRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException // Import específico para errores de IO generales
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación concreta de [ProgramaRepository] y [EpisodioRepository].
 * Se encarga de obtener los datos directamente desde la API REST de WordPress
 * utilizando Ktor como cliente HTTP. Maneja la construcción de peticiones,
 * la deserialización de respuestas JSON y la gestión centralizada de errores de red/API.
 *
 * @author Mario Alguacil Juárez
 */
class WordpressService : ProgramaRepository, EpisodioRepository {

    // URL base para la API de WordPress.
    // Ejemplo: "https://pruebas.paradigmamedia.org/wp-json/wp/v2"
    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2" //

    /**
     * Envuelve las llamadas a la API con un manejo de errores común para excepciones de Ktor y de red.
     * Convierte excepciones técnicas en excepciones personalizadas y más significativas para la aplicación.
     *
     * @param T El tipo de dato esperado como resultado de la llamada a la API.
     * @param errorMessage Un mensaje descriptivo del contexto de la llamada, usado si ocurre un error genérico.
     * @param apiCall La lambda que contiene la llamada de red real a Ktor.
     * @return El resultado de la llamada a la API si es exitosa.
     * @throws NoInternetException Si la petición falla debido a problemas de red (timeout, IO sin conexión).
     * @throws ServerErrorException Si el servidor devuelve un error (HTTP 5xx) o ocurre una excepción inesperada.
     * @throws ApiException Si la API devuelve un error del cliente (HTTP 4xx, excluyendo 404 que se maneja específicamente en algunos casos).
     * @throws CancellationException Si la corutina es cancelada.
     */
    private suspend inline fun <T> safeApiCall(errorMessage: String, crossinline apiCall: suspend () -> T): T {
        try {
            return apiCall()
        } catch (e: CancellationException) {
            // Propagar la CancellationException para permitir que las corutinas se cancelen correctamente.
            throw e
        } catch (e: HttpRequestTimeoutException) {
            // Timeout específico de Ktor (conexión, socket o request total).
            throw NoInternetException("El servidor tardó demasiado en responder. Por favor, inténtalo de nuevo o revisa tu conexión.", e)
        } catch (e: ResponseException) {
            // Errores HTTP devueltos por el servidor (4xx, 5xx).
            val statusCode = e.response.status.value
            if (statusCode >= 500) {
                throw ServerErrorException("Error en el servidor ($statusCode). Por favor, inténtalo más tarde.", e)
            } else if (e.response.status == HttpStatusCode.NotFound) { // Específico para 404
                // Esta excepción será capturada por el llamador si necesita tratar 404 de forma especial (ej. devolver null).
                throw ApiException("Recurso no encontrado (404).", e)
            } else { // Otros errores del cliente (4xx)
                throw ApiException("Error de la API: $statusCode. $errorMessage", e)
            }
        } catch (e: IOException) {
            // Errores generales de red/IO (ej. UnknownHostException si no hay DNS o conexión).
            // Ktor puede lanzar esto para problemas de conectividad antes de que se establezca una conexión HTTP.
            throw NoInternetException("No se pudo conectar. Por favor, revisa tu conexión a internet.", e)
        } catch (e: Exception) {
            // Captura genérica para cualquier otra excepción inesperada.
            throw ServerErrorException("Ocurrió un error inesperado. $errorMessage", e)
        }
    }

    /**
     * Obtiene la lista de todos los programas (términos de la taxonomía 'radio') desde la API.
     *
     * @return Una lista de objetos [Programa].
     * @throws NoInternetException, ServerErrorException, ApiException Ver [safeApiCall].
     */
    override suspend fun getProgramas(): List<Programa> {
        return safeApiCall("Error al obtener programas") {
            ktorClient.get("$baseUrl/radio") { // Endpoint para la taxonomía 'radio'
                parameter("per_page", 100) // Solicita hasta 100 programas para minimizar paginación.
            }.body() // Deserializa la respuesta a List<Programa>
        }
    }

    /**
     * Obtiene los episodios (posts) de un programa específico, con paginación.
     * Los episodios se ordenan por fecha descendente.
     *
     * @param programaId El ID del programa (término de taxonomía 'radio') para filtrar los episodios.
     * @param page El número de página a solicitar.
     * @param perPage El número de episodios por página.
     * @return Una lista de objetos [Episodio].
     * @throws NoInternetException, ServerErrorException, ApiException Ver [safeApiCall].
     */
    override suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int, perPage: Int): List<Episodio> {
        return safeApiCall("Error al obtener episodios para el programa $programaId") {
            ktorClient.get("$baseUrl/posts") { // Asumiendo que los episodios son 'posts' y se filtran por taxonomía.
                parameter("radio", programaId) // Filtra por el ID de la taxonomía 'radio'.
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date") // Ordena por fecha.
                parameter("order", "desc")   // Descendente (los más nuevos primero).
                parameter("_embed", "wp:featuredmedia,wp:term") // Incrusta datos relacionados para evitar llamadas adicionales.
            }.body()
        }
    }

    /**
     * Obtiene una lista paginada de todos los episodios (posts), ordenados por fecha descendente.
     *
     * @param page El número de página a solicitar.
     * @param perPage El número de episodios por página.
     * @return Una lista de objetos [Episodio].
     * @throws NoInternetException, ServerErrorException, ApiException Ver [safeApiCall].
     */
    override suspend fun getAllEpisodios(page: Int, perPage: Int): List<Episodio> {
        return safeApiCall("Error al obtener todos los episodios") {
            ktorClient.get("$baseUrl/posts") { // Endpoint general de posts.
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        }
    }

    /**
     * Obtiene los detalles de un episodio (post) específico por su ID.
     *
     * @param episodioId El ID del episodio a recuperar.
     * @return El objeto [Episodio] si se encuentra; null si la API devuelve un 404 (No Encontrado).
     * @throws NoInternetException, ServerErrorException (para errores > 500 o distintos de 404),
     * ApiException (para otros errores 4xx distintos de 404). Ver [safeApiCall].
     */
    override suspend fun getEpisodio(episodioId: Int): Episodio? {
        return try {
            safeApiCall("Error al obtener el episodio $episodioId") {
                ktorClient.get("$baseUrl/posts/$episodioId") { // Endpoint para un post específico por ID.
                    parameter("_embed", "wp:featuredmedia,wp:term") // Incrusta datos relacionados.
                }.body()
            }
        } catch (e: ApiException) {
            // Manejo específico para 404: si safeApiCall lanza ApiException con "Recurso no encontrado",
            // interpretamos esto como que el episodio no existe y devolvemos null.
            if (e.message?.startsWith("Recurso no encontrado") == true) {
                null
            } else {
                // Relanzamos otras ApiExceptions que no sean 404.
                throw e
            }
        }
        // Otras excepciones (NoInternetException, ServerErrorException) se propagarán desde safeApiCall.
    }

    /**
     * Realiza una búsqueda de episodios (posts) basada en un término.
     * La API de WordPress buscará en campos como título y contenido.
     *
     * @param searchTerm El término de búsqueda. Se retorna una lista vacía si es muy corto o está en blanco.
     * @return Una lista de objetos [Episodio] que coinciden con la búsqueda.
     * @throws NoInternetException, ServerErrorException, ApiException Ver [safeApiCall].
     */
    override suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        // Validación básica del término de búsqueda para evitar llamadas innecesarias a la API.
        if (searchTerm.isBlank() || searchTerm.length <= 2) { //
            return emptyList()
        }

        return safeApiCall("Error buscando episodios con término '$searchTerm'") {
            ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm) // Parámetro 'search' de la API de WordPress.
                parameter("per_page", 100) // Limita el número de resultados de búsqueda.
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        }
    }
}