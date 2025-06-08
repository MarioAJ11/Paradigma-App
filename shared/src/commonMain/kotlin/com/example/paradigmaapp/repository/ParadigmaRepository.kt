package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.cache.Database
import com.example.paradigmaapp.cache.ProgramaEntity
import com.example.paradigmaapp.cache.toDomain
import com.example.paradigmaapp.cache.toEntity
import com.example.paradigmaapp.cache.EpisodioEntity
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
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Repositorio principal de la aplicación. Gestiona la obtención de datos
 * combinando una caché local (SQLDelight) y llamadas a la red (Ktor).
 * Actúa como la única fuente de verdad para los ViewModels, abstrayendo la complejidad
 * del origen de los datos.
 *
 * @param database La instancia de la base de datos local, inyectada para gestionar la caché.
 *
 * @author Mario Alguacil Juárez
 */
class ParadigmaRepository(private val database: Database) : ProgramaRepository, EpisodioRepository {

    private val programaQueries = database.programaQueries
    private val episodioQueries = database.episodioQueries
    private val baseUrl = "https://pruebas.paradigmamedia.org/wp-json/wp/v2"

    /**
     * Obtiene la lista de programas usando una estrategia de "caché primero, luego red".
     * 1. Devuelve inmediatamente los datos guardados en la base de datos local (caché).
     * 2. Intenta actualizar la caché en segundo plano desde la red.
     * 3. Si la red falla, la app sigue funcionando con los datos cacheados.
     *
     * @return Una lista de objetos [Programa].
     */
    override suspend fun getProgramas(): List<Programa> {
        val cachedProgramas = programaQueries.selectAllProgramas()
            .executeAsList()
            .map { programaEntity: ProgramaEntity -> programaEntity.toDomain() }

        val networkProgramas = try {
            fetchProgramasFromNetwork()
        } catch (e: Exception) {
            return cachedProgramas
        }

        programaQueries.transaction {
            programaQueries.deleteAllProgramas()
            networkProgramas.forEach { programa ->
                programaQueries.insertPrograma(
                    id = programa.id.toLong(),
                    name = programa.name,
                    slug = programa.slug,
                    description = programa.description,
                    imageUrl = programa.imageUrl,
                    count = programa.count?.toLong()
                )
            }
        }

        return networkProgramas
    }

    /**
     * Obtiene los detalles de un programa específico, usando la caché.
     * @param programaId El ID del [Programa] a recuperar.
     * @return El objeto [Programa] correspondiente o null si no se encuentra.
     */
    override suspend fun getPrograma(programaId: Int): Programa? {
        return getProgramas().find { programa -> programa.id == programaId }
    }

    /**
     * Función privada que solo se encarga de la llamada de red para obtener programas.
     */
    private suspend fun fetchProgramasFromNetwork(): List<Programa> {
        return ktorClient.get("$baseUrl/radio") {
            parameter("per_page", 100)
        }.body()
    }

    /**
     * Obtiene los episodios de un programa usando una estrategia de "red primero, luego caché como fallback".
     * @return Una lista de [Episodio].
     */
    override suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int, perPage: Int): List<Episodio> {
        try {
            val networkEpisodios = ktorClient.get("$baseUrl/posts") {
                parameter("radio", programaId)
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body<List<Episodio>>()

            if (page == 1 && networkEpisodios.isNotEmpty()) {
                episodioQueries.transaction {
                    episodioQueries.deleteEpisodiosByProgramaId(programaId.toLong())
                    networkEpisodios.forEach { episodio ->
                        val entity = episodio.toEntity(programaId)
                        episodioQueries.insertEpisodio(
                            id = entity.id,
                            title = entity.title,
                            content = entity.content,
                            archiveUrl = entity.archiveUrl,
                            imageUrl = entity.imageUrl,
                            date = entity.date,
                            duration = entity.duration,
                            programaId = entity.programaId
                        )
                    }
                }
            }

            return networkEpisodios

        } catch (e: Exception) {
            val cachedEpisodios = episodioQueries
                .selectEpisodiosByProgramaId(programaId.toLong())
                .executeAsList()
                .map { episodioEntity: EpisodioEntity -> episodioEntity.toDomain() }

            if (cachedEpisodios.isNotEmpty()) {
                return cachedEpisodios
            } else {
                throw e
            }
        }
    }

    /**
     * Obtiene una lista paginada de todos los episodios, típicamente los más recientes.
     * Nota: Esta función actualmente no utiliza la caché local.
     */
    override suspend fun getAllEpisodios(page: Int, perPage: Int): List<Episodio> {
        return safeApiCall("Error al obtener todos los episodios") {
            ktorClient.get("$baseUrl/posts") {
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        }
    }

    /**
     * Obtiene los detalles de un episodio específico por su ID.
     * Nota: Esta función actualmente no utiliza la caché local.
     */
    override suspend fun getEpisodio(episodioId: Int): Episodio? {
        return try {
            safeApiCall("Error al obtener el episodio $episodioId") {
                ktorClient.get("$baseUrl/posts/$episodioId") {
                    parameter("_embed", "wp:featuredmedia,wp:term")
                }.body()
            }
        } catch (e: ApiException) {
            if (e.message?.startsWith("Recurso no encontrado") == true) {
                null
            } else {
                throw e
            }
        }
    }

    /**
     * Busca episodios basándose en un término de búsqueda.
     * Nota: Esta función actualmente no utiliza la caché local.
     */
    override suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        if (searchTerm.isBlank() || searchTerm.length <= 2) {
            return emptyList()
        }
        return safeApiCall("Error buscando episodios con término '$searchTerm'") {
            ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm)
                parameter("per_page", 100)
            }.body()
        }
    }

    /**
     * Envuelve las llamadas a la API con un manejo de errores común.
     */
    private suspend inline fun <T> safeApiCall(errorMessage: String, crossinline apiCall: suspend () -> T): T {
        try {
            return apiCall()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw NoInternetException("El servidor tardó demasiado en responder.", e)
        } catch (e: ResponseException) {
            val statusCode = e.response.status.value
            if (statusCode >= 500) {
                throw ServerErrorException("Error en el servidor ($statusCode).", e)
            } else if (e.response.status == HttpStatusCode.NotFound) {
                throw ApiException("Recurso no encontrado (404).", e)
            } else {
                throw ApiException("Error de la API: $statusCode. $errorMessage", e)
            }
        } catch (e: IOException) {
            throw NoInternetException("No se pudo conectar. Revisa tu conexión a internet.", e)
        } catch (e: Exception) {
            throw ServerErrorException("Ocurrió un error inesperado. $errorMessage", e)
        }
    }
}