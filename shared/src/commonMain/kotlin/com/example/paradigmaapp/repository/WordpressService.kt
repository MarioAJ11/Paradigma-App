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
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException


/**
 * Esta es mi implementación concreta de los repositorios. Se encarga de obtener los datos
 * directamente desde la API de WordPress utilizando Ktor como cliente HTTP.
 *
 * @author Mario Alguacil Juárez
 */
class WordpressService : ProgramaRepository, EpisodioRepository {

    // URL base de la API de WordPress con la que estoy trabajando.
    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2"

    // Función auxiliar privada para encapsular la lógica común de try-catch en las llamadas a la API.
    // Esto me ayuda a no repetir código y a centralizar el manejo de errores de red y servidor.
    private suspend inline fun <T> safeApiCall(errorMessage: String, crossinline apiCall: suspend () -> T): T {
        try {
            return apiCall()
        } catch (e: CancellationException) {
            // Propago la CancellationException para asegurar que las corutinas se puedan cancelar correctamente.
            throw e
        } catch (e: HttpRequestTimeoutException) {
            // Manejo específicamente los timeouts configurados en Ktor.
            throw NoInternetException("El servidor tardó demasiado en responder. Por favor, inténtalo de nuevo o revisa tu conexión.")
        } catch (e: ResponseException) {
            // Manejo errores HTTP que el servidor devuelve (ej. códigos de estado 4xx, 5xx).
            if (e.response.status.value >= 500) {
                throw ServerErrorException(userFriendlyMessage = "Error en el servidor (${e.response.status.value}). Por favor, inténtalo más tarde.", cause = e)
            } else if (e.response.status == HttpStatusCode.NotFound) { // Comparación con HttpStatusCode.NotFound
                // Manejo específico para errores 404 (No Encontrado).
                throw ApiException("Recurso no encontrado.")
            }
            // Para otros errores del cliente (4xx), lanzo una excepción genérica de API.
            throw ApiException("Error de la API: ${e.response.status.value}. $errorMessage")
        } catch (e: IOException) {
            // Manejo errores generales de red/IO (ej. sin internet, host inaccesible).
            // Esto capturará, por ejemplo, java.net.UnknownHostException.
            throw NoInternetException("No se pudo conectar. Por favor, revisa tu conexión a internet.")
        } catch (e: Exception) {
            // Captura para cualquier otro error inesperado.
            throw ServerErrorException(userFriendlyMessage = "Ocurrió un error inesperado. $errorMessage", cause = e)
        }
    }

    /**
     * Obtiene la lista de todos los programas desde la API.
     * Utiliza el endpoint /radio.
     */
    override suspend fun getProgramas(): List<Programa> {
        return safeApiCall("Error al obtener programas") {
            val programas: List<Programa> = ktorClient.get("$baseUrl/radio") {
                parameter("per_page", 100) // Solicito hasta 100 programas.
            }.body()
            programas
        }
    }

    /**
     * Obtiene los episodios de un programa específico, con paginación.
     * Se ordena por fecha descendente para mostrar los más nuevos primero.
     * Incluye datos embebidos como la imagen destacada y los términos de taxonomía.
     */
    override suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int, perPage: Int): List<Episodio> {
        return safeApiCall("Error al obtener episodios para el programa $programaId") {
            val response = ktorClient.get("$baseUrl/posts") {
                parameter("radio", programaId)
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }
            val episodios: List<Episodio> = response.body()
            episodios
        }
    }

    /**
     * Obtiene una lista de todos los episodios, paginada.
     * Útil para, por ejemplo, una vista de "últimos episodios".
     */
    override suspend fun getAllEpisodios(page: Int, perPage: Int): List<Episodio> {
        return safeApiCall("Error al obtener todos los episodios") {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            episodios
        }
    }

    /**
     * Obtiene los detalles de un episodio específico por su ID.
     * Devuelve null si el episodio no se encuentra (ej. error 404).
     */
    override suspend fun getEpisodio(episodioId: Int): Episodio? {
        try {
            return safeApiCall("Error al obtener el episodio $episodioId") {
                val episodio: Episodio = ktorClient.get("$baseUrl/posts/$episodioId") {
                    parameter("_embed", "wp:featuredmedia,wp:term") // Solicito datos embebidos.
                }.body()
                episodio
            }
        } catch (e: ApiException) {
            // Si safeApiCall lanzó ApiException por un 404, aquí lo manejo específicamente para devolver null.
            if (e.message == "Recurso no encontrado.") {
                return null
            }
            // Relanzo otras ApiExceptions que no sean simplemente "no encontrado".
            throw e
        }
    }

    /**
     * Realiza una búsqueda de episodios basada en un término.
     * La búsqueda se hace en el servidor de WordPress.
     */
    override suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        // Validación básica para el término de búsqueda.
        if (searchTerm.isBlank() || searchTerm.length <= 2) return emptyList()

        return safeApiCall("Error buscando episodios con término '$searchTerm'") {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm)
                parameter("per_page", 100) // Limito los resultados de búsqueda.
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            episodios
        }
    }
}