package com.example.paradigmaapp.sdk

import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.ParadigmaRepository

/**
 * Clase "Facade" que actúa como el punto de entrada principal para las plataformas nativas.
 * No crea sus propias dependencias, sino que las recibe en su constructor.
 *
 * @param repository El repositorio que contiene toda la lógica de datos.
 * @author Mario Alguacil Juárez
 */
class ParadigmaSDK(private val repository: ParadigmaRepository) {

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
}