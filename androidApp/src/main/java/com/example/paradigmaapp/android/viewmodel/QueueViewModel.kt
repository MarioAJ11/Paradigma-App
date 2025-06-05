package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar la cola de reproducción de episodios.
 * Responsable de:
 * - Cargar y guardar el estado de la cola (lista de IDs de episodios) usando [AppPreferences].
 * - Obtener los detalles completos de los [Episodio]s en la cola para la UI, utilizando
 * una caché local (`allAvailableEpisodesCache`) o el [EpisodioRepository].
 * - Permitir añadir, eliminar y reordenar (implícitamente al eliminar el actual) episodios en la cola.
 *
 * @property appPreferences Instancia de [AppPreferences] para la persistencia de la cola.
 * @property episodioRepository Repositorio para obtener detalles de episodios.
 *
 * @author Mario Alguacil Juárez
 */
class QueueViewModel(
    private val appPreferences: AppPreferences,
    private val episodioRepository: EpisodioRepository
) : ViewModel() {

    // StateFlow para los IDs de los episodios en la cola. Esta es la fuente primaria de verdad para el orden.
    private val _queueEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val queueEpisodeIds: StateFlow<List<Int>> = _queueEpisodeIds.asStateFlow()

    // StateFlow para los objetos Episodio completos de la cola, derivados de _queueEpisodeIds.
    private val _queueEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val queueEpisodios: StateFlow<List<Episodio>> = _queueEpisodios.asStateFlow()

    // Caché de todos los episodios disponibles, proporcionada por MainViewModel.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    init {
        loadQueueState() // Carga el estado de la cola al iniciar.
    }

    /**
     * Establece la lista de todos los episodios disponibles en la aplicación.
     * Esta caché se utiliza para construir la lista de [_queueEpisodios]
     * de forma más eficiente.
     *
     * @param episodes Lista de todos los [Episodio]s disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        viewModelScope.launch(Dispatchers.IO) {
            updateQueueEpisodiosListFromIds()
        }
    }

    /**
     * Carga la lista de IDs de la cola desde [AppPreferences] y luego
     * actualiza la lista de objetos [Episodio] completos.
     */
    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = appPreferences.loadEpisodeQueue()
            updateQueueEpisodiosListFromIds()
        }
    }

    /** Guarda la lista actual de IDs de la cola en [AppPreferences]. */
    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveEpisodeQueue(_queueEpisodeIds.value)
        }
    }

    /**
     * Actualiza `_queueEpisodios` (lista de objetos [Episodio]) basada en `_queueEpisodeIds`.
     * Intenta obtener los detalles del episodio desde `allAvailableEpisodesCache` primero;
     * si no se encuentra, lo busca a través de [episodioRepository].
     * El orden de `_queueEpisodios` reflejará el orden de `_queueEpisodeIds`.
     */
    private suspend fun updateQueueEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        _queueEpisodeIds.value.forEach { id ->
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                try {
                    episodioRepository.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    }
                    // Si no se encuentra el episodio, no se añade.
                } catch (e: Exception) {
                    // Considerar registrar el error. No se añade si hay excepción.
                }
            }
        }
        // Asegurar que los objetos Episodio en _queueEpisodios sigan el orden de _queueEpisodeIds.
        // Lo hacemos reconstruyendo la lista en el orden de los IDs.
        withContext(Dispatchers.Main) { // Actualizar el StateFlow en el hilo principal
            _queueEpisodios.value = episodeDetailsList
        }
    }

    /**
     * Añade un episodio al final de la cola de reproducción si aún no está presente.
     *
     * @param episodio El [Episodio] a añadir.
     */
    fun addEpisodeToQueue(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de lista y guardado en IO
            if (!_queueEpisodeIds.value.contains(episodio.id)) {
                val newIds = _queueEpisodeIds.value + episodio.id
                _queueEpisodeIds.value = newIds
                saveQueueState() // Guardar la nueva lista de IDs.

                // Actualizar la lista de objetos Episodio
                val newEpisodios = _queueEpisodios.value.toMutableList()
                if (newEpisodios.none { it.id == episodio.id }) { // Doble check por si acaso
                    newEpisodios.add(episodio)
                }
                withContext(Dispatchers.Main) {
                    _queueEpisodios.value = newEpisodios
                }
            }
        }
    }

    /**
     * Elimina un episodio de la cola de reproducción.
     *
     * @param episodio El [Episodio] a eliminar.
     */
    fun removeEpisodeFromQueue(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de lista y guardado en IO
            if (_queueEpisodeIds.value.contains(episodio.id)) {
                _queueEpisodeIds.value = _queueEpisodeIds.value - episodio.id
                saveQueueState() // Guardar la nueva lista de IDs.

                // Actualizar la lista de objetos Episodio.
                withContext(Dispatchers.Main) {
                    _queueEpisodios.value = _queueEpisodios.value.filterNot { it.id == episodio.id }
                }
            }
        }
    }

    /**
     * Elimina el episodio que se acaba de reproducir de la cola y devuelve el siguiente episodio.
     * Esta función se llama típicamente desde [MainViewModel] cuando un episodio finaliza.
     *
     * @param playedEpisodeId El ID del episodio que acaba de terminar de reproducirse.
     * @return El siguiente [Episodio] en la cola para reproducir, o `null` si la cola está vacía
     * o el siguiente episodio no se puede cargar.
     */
    suspend fun dequeueNextEpisode(playedEpisodeId: Int): Episodio? = withContext(Dispatchers.IO) {
        val currentIds = _queueEpisodeIds.value.toMutableList()
        val playedIndex = currentIds.indexOf(playedEpisodeId)

        if (playedIndex != -1) {
            currentIds.removeAt(playedIndex)
            _queueEpisodeIds.value = currentIds
            saveQueueState() // Guardar el estado de IDs actualizado.

            // Reconstruir la lista de objetos Episodio en el orden correcto.
            updateQueueEpisodiosListFromIds() // Esto actualiza _queueEpisodios en el hilo principal.

            // Devolver el primer elemento de la lista de objetos recién actualizada.
            return@withContext _queueEpisodios.value.firstOrNull()
        }
        // Si el playedEpisodeId no estaba en la cola (caso raro),
        // o si la cola estaba vacía después de quitarlo.
        return@withContext _queueEpisodios.value.firstOrNull()
    }


    /** Limpia completamente la cola de reproducción. */
    fun clearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = emptyList()
            _queueEpisodios.value = emptyList() // Limpiar también la lista de objetos.
            saveQueueState()
        }
    }
}