package com.example.paradigmaapp.repository

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.cache.Database
import com.example.paradigmaapp.cache.toDomain
import com.example.paradigmaapp.cache.toEntity
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
 * Repositorio principal que gestiona datos de programas y episodios.
 *
 * @param database Instancia de la base de datos local para la gestión de la caché.
 * @param baseUrl La URL base para la API de WordPress, obtenida de la configuración.
 */
class ParadigmaRepository(
    private val database: Database,
    private val baseUrl: String
) : ProgramaRepository, EpisodioRepository {

    private val programaQueries = database.programaQueries
    private val episodioQueries = database.episodioQueries

    // --- FUNCIONES DE PROGRAMA ---

    /**
     * Recupera una lista de todos los programas disponibles desde la fuente de datos.
     *
     * @return Una [List] de objetos [Programa].
     */
    override suspend fun getProgramas(): List<Programa> {
        val cachedProgramas = programaQueries.selectAllProgramas()
            .executeAsList()
            .map { it.toDomain() }

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
     * Recupera un único programa específico por su ID.
     *
     * @param programaId El ID del programa a recuperar.
     * @return El objeto [Programa] correspondiente o null si no se encuentra.
     */
    override suspend fun getPrograma(programaId: Int): Programa? {
        return getProgramas().find { it.id == programaId }
    }

    /**
     * Recupera la lista de programas desde la API de WordPress.
     *
     * @return Una [List] de objetos [Programa].
     */
    private suspend fun fetchProgramasFromNetwork(): List<Programa> {
        return ktorClient.get("$baseUrl/radio") {
            parameter("per_page", 100)
        }.body()
    }

    // --- FUNCIONES DE EPISODIO ---

    /**
     * Recupera una lista paginada de episodios para un ID de programa específico.
     *
     * @param programaId El ID del programa para el cual se recuperarán los episodios.
     * @param page El número de página para la paginación (por defecto es 1).
     * @param perPage El número de episodios a recuperar por página (por defecto es 100).
     * La API de WordPress puede tener un límite máximo para este valor (usualmente 100).
     * @return Una [List] de objetos [Episodio].
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API.
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
                .map { it.toDomain() }

            if (cachedEpisodios.isNotEmpty()) {
                return cachedEpisodios
            } else {
                throw e
            }
        }
    }

    /**
     * Recupera una lista paginada de todos los episodios, típicamente los más recientes.
     * Útil para una vista general de "últimos episodios".
     *
     * @param page El número de página para la paginación (por defecto es 1).
     * @param perPage El número de episodios a recuperar por página (por defecto es 20).
     * @return Una [List] de objetos [Episodio].
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API.
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

    // --- FUNCIONES DE BÚSQUEDA ---

    /**
     * Busca episodios en la caché local de forma síncrona.
     * Esta función proporciona resultados inmediatos para mejorar la experiencia de usuario.
     * Filtra los episodios cacheados cuyo título o contenido contengan el término de búsqueda.
     *
     * @param searchTerm El texto a buscar en la caché.
     * @return Una lista de [Episodio] que coinciden con la búsqueda en la caché local.
     */
    fun buscarEpisodiosEnCache(searchTerm: String): List<Episodio> {
        if (searchTerm.length < 3) return emptyList()

        // La forma más eficiente sería con una consulta SQLDelight `LIKE`.
        // Como alternativa robusta, filtramos en memoria los episodios ya cacheados.
        return episodioQueries.selectAllEpisodios()
            .executeAsList()
            .filter {
                it.title.contains(searchTerm, ignoreCase = true) ||
                        it.content?.contains(searchTerm, ignoreCase = true) == true
            }
            .map { it.toDomain() }
    }

    /**
     * Busca episodios en la red de forma optimizada.
     * Utiliza el parámetro '_fields' de la API de WordPress para solicitar solo los
     * datos necesarios para la vista de lista, excluyendo campos pesados como 'content'.
     * Esto reduce drásticamente el tamaño de la respuesta y mejora la velocidad.
     *
     * @param searchTerm El texto a buscar en la red.
     * @return Una lista de objetos [Episodio] con datos parciales pero suficientes para la lista.
     */
    override suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        if (searchTerm.isBlank() || searchTerm.length <= 2) {
            return emptyList()
        }

        // Definimos los campos que SÍ necesitamos para la lista de resultados.
        // Excluimos deliberadamente 'content' y 'excerpt' por ser los más pesados.
        val camposNecesarios = "id,date_gmt,slug,title,meta,url_del_podcast,radio,_links,_embedded"

        return safeApiCall("Error buscando episodios con término '$searchTerm'") {
            ktorClient.get("$baseUrl/posts") {
                parameter("search", searchTerm)
                parameter("per_page", 100)
                // Pedimos a la API que solo nos devuelva los campos que hemos definido.
                parameter("_fields", camposNecesarios)
                // También pedimos que incruste la imagen destacada y los programas para no hacer más llamadas.
                parameter("_embed", "wp:featuredmedia,wp:term")
            }.body()
        }
    }

    // --- MANEJO DE ERRORES ---

    /**
     * Realiza una llamada a la API y maneja posibles errores.
     *
     * @param errorMessage Mensaje de error a mostrar en caso de excepción.
     * @param apiCall Lambda que contiene la llamada a la API.
     * @return El resultado de la llamada a la API.
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API.
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