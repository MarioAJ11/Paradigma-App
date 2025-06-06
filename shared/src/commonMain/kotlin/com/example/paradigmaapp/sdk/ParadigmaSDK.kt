package com.example.paradigmaapp.sdk

import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.WordpressService

/**
 * Clase "Facade" que actúa como el punto de entrada principal para las plataformas nativas (iOS, etc.).
 * Encapsula la lógica del repositorio y expone funciones simples y claras para ser consumidas
 * desde Swift y otros lenguajes.
 *
 * @author Mario Alguacil Juárez
 */
class ParadigmaSDK {

    // Por simplicidad, creamos una instancia de nuestro servicio directamente.
    // En una app más compleja, esto se podría manejar con inyección de dependencias (ej. Koin).
    private val repository = WordpressService()

    /**
     * Obtiene la lista de todos los programas.
     * Es una función 'suspend' porque realiza una operación de red asíncrona.
     *
     * @return Una lista de objetos [Programa].
     * @throws Exception si ocurre un error de red o de la API.
     */
    @Throws(Exception::class) // Esta anotación es importante para la interoperabilidad con Swift.
    suspend fun getProgramas(): List<Programa> {
        return repository.getProgramas()
    }

    // TODO
}