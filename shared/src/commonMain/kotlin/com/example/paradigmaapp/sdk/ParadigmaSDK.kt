package com.example.paradigmaapp.sdk

import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.ParadigmaRepository

/**
 * Clase "Facade" que actúa como el punto de entrada principal para las plataformas nativas.
 * Expone de forma sencilla las funciones del repositorio al código de iOS y Android.
 *
 * @param repository El repositorio que contiene toda la lógica de datos.
 * @author Mario Alguacil Juárez
 */
class ParadigmaSDK(private val repository: ParadigmaRepository) {

    /**
     * Obtiene la lista de todos los programas.
     * @return Una lista de objetos [Programa].
     * @throws Exception para cualquier error de red o de la API.
     */
    @Throws(Exception::class)
    suspend fun getProgramas(): List<Programa> {
        return repository.getProgramas()
    }

    /**
     * Obtiene los detalles de un único programa específico por su ID.
     * @param programaId El ID del programa a recuperar.
     * @return El objeto [Programa] correspondiente o null si no se encuentra.
     */
    @Throws(Exception::class)
    suspend fun getPrograma(programaId: Int): Programa? {
        return repository.getPrograma(programaId)
    }

    /**
     * Obtiene una lista paginada de episodios para un programa específico.
     * @param programaId El ID del programa.
     * @param page El número de página a solicitar.
     * @return Una lista de objetos [Episodio].
     */
    @Throws(Exception::class)
    suspend fun getEpisodiosPorPrograma(programaId: Int, page: Int): List<Episodio> {
        return repository.getEpisodiosPorPrograma(programaId = programaId, page = page, perPage = 20)
    }

    /**
     * Obtiene los detalles completos de un único episodio por su ID.
     * @param episodioId El ID del episodio a recuperar.
     * @return El objeto [Episodio] correspondiente si se encuentra, o null.
     */
    @Throws(Exception::class)
    suspend fun getEpisodio(episodioId: Int): Episodio? {
        return repository.getEpisodio(episodioId = episodioId)
    }

    /**
     * Busca episodios en la red basándose en un término de búsqueda.
     * @param searchTerm El texto a buscar.
     * @return Una lista de [Episodio] que coinciden con la búsqueda.
     */
    @Throws(Exception::class)
    suspend fun buscarEpisodios(searchTerm: String): List<Episodio> {
        return repository.buscarEpisodios(searchTerm)
    }
}