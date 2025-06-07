package com.example.paradigmaapp.sdk

import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.WordpressService

/**
 * Clase "Facade" que actúa como el punto de entrada principal para las plataformas nativas iOS.
 * Encapsula la lógica del repositorio y expone funciones simples y claras para ser consumidas
 * desde Swift y otros lenguajes.
 *
 * @author Mario Alguacil Juárez
 */
class ParadigmaSDK {
    private val repository = WordpressService()

    /**
     * Obtiene la lista de todos los programas.
     * Es una función 'suspend' porque realiza una operación de red asíncrona.
     *
     * @return Una lista de objetos [Programa].
     * @throws Exception si ocurre un error de red o de la API.
     */
    @Throws(Exception::class)
    suspend fun getProgramas(): List<Programa> {
        return repository.getProgramas()
    }

    // TODO: Con todo
}