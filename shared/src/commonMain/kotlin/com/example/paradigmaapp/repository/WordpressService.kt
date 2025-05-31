package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import io.ktor.client.call.*
import io.ktor.client.plugins.* // Necesario para HttpRequestTimeoutException y ResponseException
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.errors.IOException // Para otros errores de IO
import kotlin.coroutines.cancellation.CancellationException

class WordpressService {

    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2"

    private suspend inline fun <T> safeApiCall(errorMessage: String, crossinline apiCall: suspend () -> T): T {
        try {
            return apiCall()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) { // Capturar el timeout específico de Ktor
            println("WordpressService: $errorMessage. Timeout en la petición HTTP: ${e.message}")
            // Lanzamos NoInternetException pero con un mensaje más adecuado para timeout
            throw NoInternetException("El servidor tardó demasiado en responder. Inténtalo de nuevo o revisa tu conexión.")
        } catch (e: ResponseException) {
            println("WordpressService: $errorMessage. Error HTTP: ${e.response.status.value}. Message: ${e.message}")
            if (e.response.status.value >= 500) {
                throw ServerErrorException(userFriendlyMessage = "Error en el servidor (${e.response.status.value}). Inténtalo más tarde.", cause = e)
            } else if (e.response.status.value == 404) {
                throw ApiException("Recurso no encontrado.")
            }
            throw ApiException("Error de la API: ${e.response.status.value}. $errorMessage")
        } catch (e: IOException) { // Errores de red más generales (ej. host no encontrado)
            println("WordpressService: $errorMessage. Error de Red/IO: ${e.message}")
            throw NoInternetException("No se pudo conectar. Revisa tu conexión a internet.")
        } catch (e: Exception) {
            println("WordpressService: $errorMessage. Error inesperado: ${e.message}")
            e.printStackTrace()
            throw ServerErrorException(userFriendlyMessage = "Ocurrió un error inesperado. $errorMessage", cause = e)
        }
    }

    // ... el resto de tus funciones (getProgramas, getEpisodiosPorPrograma, etc.) no necesitan cambiar ...
    // solo asegúrate de que usan safeApiCall

    suspend fun getProgramas(): List<Programa> {
        println("WordpressService: Intentando obtener programas desde $baseUrl/radio")
        return safeApiCall("Error al obtener programas") {
            val programas: List<Programa> = ktorClient.get("$baseUrl/radio") {
                parameter("per_page", 100)
            }.body()
            println("WordpressService: Recibidos ${programas.size} programas.")
            programas
        }
    }

    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int = 1, perPage: Int = 100): List<Episodio> {
        val queryParams = buildString {
            append("?radio=${programaId}")
            append("&page=${page}")
            append("&per_page=${perPage}")
            append("&orderby=date")
            append("&order=desc")
            append("&_embed=wp:featuredmedia,wp:term")
        }
        val constructedUrlForLog = "${baseUrl}/posts$queryParams"
        println("WordpressService: Intentando obtener episodios para programaId: $programaId")
        println("WordpressService: URL que se intentará (construida para log): $constructedUrlForLog")

        return safeApiCall("Error al obtener episodios para programaId $programaId") {
            val response = ktorClient.get("$baseUrl/posts") {
                parameter("radio", programaId)
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }
            val episodios: List<Episodio> = response.body()
            println("WordpressService: Recibidos ${episodios.size} episodios para programaId: $programaId.")
            episodios
        }
    }

    suspend fun getAllEpisodios(page: Int = 1, perPage: Int = 20): List<Episodio> {
        println("WordpressService: Intentando obtener todos los episodios. Página: $page, PorPágina: $perPage")
        return safeApiCall("Error al obtener todos los episodios") {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            println("WordpressService: Recibidos ${episodios.size} episodios en getAllEpisodios.")
            episodios
        }
    }

    suspend fun getEpisodio(episodioId: Int): Episodio? {
        println("WordpressService: Intentando obtener episodio con ID: $episodioId")
        try {
            return safeApiCall("Error al obtener episodio ID $episodioId") {
                val episodio: Episodio = ktorClient.get("$baseUrl/posts/$episodioId") {
                    parameter("_embed", "wp:featuredmedia,wp:term")
                }.body()
                println("WordpressService: Episodio ID $episodioId obtenido: ${episodio.title}")
                episodio
            }
        } catch (e: ApiException) {
            if (e.message?.contains("Recurso no encontrado", ignoreCase = true) == true || e.message?.contains("404") == true) {
                println("WordpressService: Episodio $episodioId no encontrado (404).")
                return null
            }
            throw e
        }
    }

    suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        if (searchTerm.isBlank() || searchTerm.length <= 2) return emptyList()
        println("WordpressService: Buscando episodios con término: '$searchTerm'")
        val constructedUrlForLog = "$baseUrl/posts?search=$searchTerm&per_page=100&_embed=wp:featuredmedia,wp:term" // Para logging
        println("WordpressService: URL de búsqueda (log): $constructedUrlForLog")
        return safeApiCall("Error buscando episodios con término '$searchTerm'") {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm)
                parameter("per_page", 100)
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            println("WordpressService: Encontrados ${episodios.size} episodios para búsqueda '$searchTerm'.")
            episodios
        }
    }
}