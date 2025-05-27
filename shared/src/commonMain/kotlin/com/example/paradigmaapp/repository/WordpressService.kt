package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Servicio para interactuar con la API REST de WordPress para obtener programas y episodios.
 * Utiliza la URL: https://pruebas.paradigmamedia.org/
 *
 * @author Mario Alguacil Juárez
 */
class WordpressService {

    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2"

    /**
     * Obtiene la lista de programas (términos de la taxonomía 'radio').
     *
     * @return Una lista de [Programa].
     * @throws Exception Si ocurre un error durante la petición.
     */
    suspend fun getProgramas(): List<Programa> {
        return ktorClient.get("$baseUrl/radio") { // Endpoint para la taxonomía 'radio'
            parameter("per_page", 100) // Puedes ajustar la cantidad de programas a obtener
            // Los campos id, name, slug, description, count vienen por defecto para taxonomías
        }.body()
    }

    /**
     * Obtiene la lista de episodios (posts) para un programa específico.
     *
     * @param programaId El ID del programa (término de la taxonomía 'radio').
     * @param page El número de página a obtener.
     * @param perPage El número de episodios por página.
     * @return Una lista de [Episodio].
     * @throws Exception Si ocurre un error durante la petición.
     */
    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int = 1, perPage: Int = 10): List<Episodio> {
        return try {
            ktorClient.get("$baseUrl/posts") { // Endpoint para los posts (que usas como episodios)
                parameter("radio", programaId) // Filtra episodios por el ID de la taxonomía 'radio'
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date") // Ordenar por fecha
                parameter("order", "desc")   // Los más recientes primero
                parameter("_embed", "wp:featuredmedia,wp:term") // Embeber imagen destacada y términos de taxonomía
            }.body()
        } catch (e: Exception) {
            println("Error fetching episodios for programa $programaId: ${e.message}")
            throw e
        }
    }

    /**
     * Obtiene todos los episodios (posts) de forma paginada.
     *
     * @param page El número de página a obtener.
     * @param perPage El número de episodios por página.
     * @return Una lista de [Episodio].
     * @throws Exception Si ocurre un error durante la petición.
     */
    suspend fun getAllEpisodios(page: Int = 1, perPage: Int = 20): List<Episodio> {
        return ktorClient.get("$baseUrl/posts") { // Endpoint para los posts
            parameter("page", page)
            parameter("per_page", perPage)
            parameter("orderby", "date")
            parameter("order", "desc")
            parameter("_embed", "wp:featuredmedia,wp:term")
        }.body()
    }


    /**
     * Obtiene un episodio (post) específico por su ID.
     *
     * @param episodioId El ID del episodio (post).
     * @return El [Episodio] o null si no se encuentra o hay error.
     * @throws Exception Si ocurre un error grave durante la petición (diferente de 404).
     */
    suspend fun getEpisodio(episodioId: Int): Episodio? {
        return try {
            ktorClient.get("$baseUrl/posts/$episodioId") { // Endpoint para un post específico
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                println("Episodio $episodioId no encontrado (404).")
                return null
            }
            println("ClientRequestException fetching episodio $episodioId: ${e.response.status} ${e.message}")
            throw e
        } catch (e: Exception) {
            println("Generic error fetching episodio $episodioId: ${e.message}")
            throw e
        }
    }

    /**
     * Busca episodios (posts) que coincidan con un término de búsqueda.
     * WordPress busca por defecto en título y contenido.
     *
     * @param searchTerm El término a buscar.
     * @return Una lista de [Episodio] que coinciden con la búsqueda.
     * @throws Exception Si ocurre un error durante la petición.
     */
    suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        if (searchTerm.isBlank()) return emptyList()
        return try {
            ktorClient.get("$baseUrl/posts") { // Endpoint para los posts
                parameter("search", searchTerm)
                parameter("per_page", 50) // Limita el número de resultados de búsqueda
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        } catch (e: Exception) {
            println("Error searching episodios with term '$searchTerm': ${e.message}")
            throw e
        }
    }
}