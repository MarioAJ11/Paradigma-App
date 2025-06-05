package com.example.paradigmaapp.repository.contracts

import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException

/**
 * Define los contratos (interfaces) para los repositorios de la aplicación.
 * Estos contratos establecen las operaciones de obtención de datos disponibles,
 * permitiendo que las capas superiores (como los ViewModels) dependan de estas
 * abstracciones en lugar de implementaciones concretas.
 *
 * @author Mario Alguacil Juárez
 */

/**
 * Contrato para el repositorio encargado de obtener datos relacionados con los [Programa]s.
 * Define las operaciones para acceder a la información de los programas.
 */
interface ProgramaRepository {
    /**
     * Recupera una lista de todos los programas disponibles desde la fuente de datos.
     *
     * @return Una [List] de objetos [Programa].
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API no cubiertos por las anteriores.
     */
    suspend fun getProgramas(): List<Programa>
}

/**
 * Contrato para el repositorio encargado de obtener y buscar datos de los [Episodio]s.
 * Define las operaciones para acceder y gestionar la información de los episodios.
 */
interface EpisodioRepository {
    /**
     * Recupera una lista paginada de episodios para un ID de programa específico.
     *
     * @param programaId El ID del [Programa] para el cual se recuperarán los episodios.
     * @param page El número de página para la paginación (por defecto es 1).
     * @param perPage El número de episodios a recuperar por página (por defecto es 100).
     * La API de WordPress puede tener un límite máximo para este valor (usualmente 100).
     * @return Una [List] de objetos [Episodio].
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API.
     */
    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int = 1, perPage: Int = 100): List<Episodio>

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
    suspend fun getAllEpisodios(page: Int = 1, perPage: Int = 20): List<Episodio>

    /**
     * Recupera un único episodio específico por su ID.
     *
     * @param episodioId El ID del [Episodio] a recuperar.
     * @return El objeto [Episodio] correspondiente si se encuentra;
     * null si el recurso no fue encontrado (ej. error 404 manejado por la implementación).
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor (distinto de 404).
     * @throws ApiException Para otros errores específicos de la API (distinto de 404).
     */
    suspend fun getEpisodio(episodioId: Int): Episodio?

    /**
     * Busca episodios basándose en un término de búsqueda proporcionado.
     * La implementación decidirá en qué campos del episodio se realiza la búsqueda (ej. título, contenido).
     *
     * @param searchTerm La cadena de texto a utilizar para la búsqueda.
     * @return Una [List] de objetos [Episodio] que coinciden con el término de búsqueda.
     * Puede devolver una lista vacía si no hay coincidencias.
     * @throws NoInternetException Si no hay conexión a internet o la red falla.
     * @throws ServerErrorException Si ocurre un error en el servidor durante la petición.
     * @throws ApiException Para otros errores específicos de la API.
     */
    suspend fun buscarEpisodios(searchTerm: String): List<Episodio>
}