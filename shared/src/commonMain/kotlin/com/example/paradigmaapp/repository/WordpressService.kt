package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class WordpressService {

    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2"

    suspend fun getProgramas(): List<Programa> {
        println("WordpressService: Intentando obtener programas desde $baseUrl/radio")
        return try {
            val programas: List<Programa> = ktorClient.get("$baseUrl/radio") {
                parameter("per_page", 100)
            }.body()
            println("WordpressService: Recibidos ${programas.size} programas.")
            programas
        } catch (e: Exception) {
            println("WordpressService: ERROR al obtener programas. Excepción: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int = 1, perPage: Int = 100): List<Episodio> {
        // Construir la URL para el log de forma manual y segura
        val queryParams = buildString {
            append("?radio=${programaId}")
            append("&page=${page}")
            append("&per_page=${perPage}")
            append("&orderby=date")
            append("&order=desc")
            append("&_embed=wp:featuredmedia,wp:term")
        }
        // Usa baseUrl que ya tiene el path /wp-json/wp/v2
        val constructedUrlForLog = "${baseUrl}/posts$queryParams"


        println("WordpressService: Intentando obtener episodios para programaId: $programaId")
        println("WordpressService: URL que se intentará (construida para log): $constructedUrlForLog")

        return try {
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
            if (episodios.isNotEmpty()) {
                // Acceso seguro por si la lista está vacía a pesar de isNotEmpty (aunque no debería pasar)
                episodios.firstOrNull()?.let { primerEpisodio ->
                    println("WordpressService: Primer episodio recibido: ${primerEpisodio.title} (ID: ${primerEpisodio.id})")
                }
            }
            episodios
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            println("WordpressService: ERROR HTTP al obtener episodios para programaId $programaId. Status: ${e.response.status}. URL aproximada: $constructedUrlForLog. Mensaje: ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("WordpressService: ERROR GENÉRICO al obtener episodios para programaId $programaId. URL aproximada: $constructedUrlForLog. Excepción: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getAllEpisodios(page: Int = 1, perPage: Int = 20): List<Episodio> {
        println("WordpressService: Intentando obtener todos los episodios. Página: $page, PorPágina: $perPage")
        return try {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            println("WordpressService: Recibidos ${episodios.size} episodios en getAllEpisodios.")
            episodios
        } catch (e: Exception) {
            println("WordpressService: ERROR al obtener todos los episodios. Excepción: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getEpisodio(episodioId: Int): Episodio? {
        println("WordpressService: Intentando obtener episodio con ID: $episodioId")
        return try {
            val episodio: Episodio? = ktorClient.get("$baseUrl/posts/$episodioId") {
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            println("WordpressService: Episodio ID $episodioId obtenido: ${episodio?.title}")
            episodio
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                println("WordpressService: Episodio $episodioId no encontrado (404).")
                return null
            }
            println("WordpressService: ClientRequestException fetching episodio $episodioId: ${e.response.status} ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("WordpressService: Generic error fetching episodio $episodioId: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        if (searchTerm.isBlank()) return emptyList()
        println("WordpressService: Buscando episodios con término: '$searchTerm'")
        return try {
            val episodios: List<Episodio> = ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm)
                parameter("per_page", 100) // Limita el número de resultados de búsqueda
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
            println("WordpressService: Encontrados ${episodios.size} episodios para búsqueda '$searchTerm'.")
            episodios
        } catch (e: Exception) {
            println("WordpressService: Error buscando episodios con término '$searchTerm': ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}