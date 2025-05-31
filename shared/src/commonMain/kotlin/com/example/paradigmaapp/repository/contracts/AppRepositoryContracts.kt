package com.example.paradigmaapp.repository.contracts

import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa

/**
 * Defino el contrato para obtener los datos de los programas.
 * Esta interfaz me ayuda a invertir las dependencias, haciendo que los ViewModels
 * dependan de esta abstracción y no de una implementación concreta, lo cual
 * mejora la modularidad y la capacidad de prueba del sistema.
 *
 * @author Mario Alguacil Juárez
 */
interface ProgramaRepository {
    /**
     * Recupera una lista de todos los programas disponibles desde la fuente de datos.
     *
     * @return Una lista de objetos [Programa].
     * @throws NoInternetException Si no hay conexión a internet.
     * @throws ServerErrorException Si ocurre un error en el servidor.
     * @throws ApiException Para otros errores relacionados con la API.
     */
    suspend fun getProgramas(): List<Programa>
}

/**
 * Defino el contrato para obtener y buscar los datos de los episodios.
 * Con esta interfaz, promuevo un acoplamiento más bajo en el sistema, permitiendo
 * que diferentes implementaciones de fuentes de datos puedan ser utilizadas
 * sin cambiar la lógica de los ViewModels.
 *
 * @author Mario Alguacil Juárez
 */
interface EpisodioRepository {
    /**
     * Recupera los episodios para un ID de programa específico, con paginación.
     *
     * @param programaId El ID del programa para el cual se recuperarán los episodios.
     * @param page El número de página para la paginación (por defecto es 1).
     * @param perPage El número de episodios a recuperar por página (por defecto es 100).
     * @return Una lista de objetos [Episodio].
     * @throws NoInternetException Si no hay conexión a internet.
     * @throws ServerErrorException Si ocurre un error en el servidor.
     * @throws ApiException Para otros errores relacionados con la API.
     */
    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int = 1, perPage: Int = 100): List<Episodio>

    /**
     * Recupera todos los episodios, con paginación.
     * Típicamente se usa para una lista inicial de los episodios más recientes.
     *
     * @param page El número de página para la paginación (por defecto es 1).
     * @param perPage El número de episodios a recuperar por página (por defecto es 20).
     * @return Una lista de objetos [Episodio].
     * @throws NoInternetException Si no hay conexión a internet.
     * @throws ServerErrorException Si ocurre un error en el servidor.
     * @throws ApiException Para otros errores relacionados con la API.
     */
    suspend fun getAllEpisodios(page: Int = 1, perPage: Int = 20): List<Episodio>

    /**
     * Recupera un único episodio por su ID.
     *
     * @param episodioId El ID del episodio a recuperar.
     * @return El objeto [Episodio] si se encuentra, o null si el recurso no fue encontrado (ej. error 404).
     * @throws NoInternetException Si no hay conexión a internet.
     * @throws ServerErrorException Si ocurre un error en el servidor (distinto de 404).
     * @throws ApiException Para otros errores relacionados con la API (distinto de 404).
     */
    suspend fun getEpisodio(episodioId: Int): Episodio?

    /**
     * Busca episodios basándose en un término de búsqueda.
     *
     * @param searchTerm La cadena de texto a buscar en títulos, contenido, etc., de los episodios.
     * @return Una lista de objetos [Episodio] que coinciden con el término de búsqueda.
     * @throws NoInternetException Si no hay conexión a internet.
     * @throws ServerErrorException Si ocurre un error en el servidor.
     * @throws ApiException Para otros errores relacionados con la API.
     */
    suspend fun buscarEpisodios(searchTerm: String): List<Episodio>
}