package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences // Tu clase de preferencias
import com.example.paradigmaapp.model.Episodio // Modelo del módulo shared
import com.example.paradigmaapp.repository.WordpressService // Servicio del módulo shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel para gestionar la cola de reproducción de episodios.
 *
 * @property appPreferences Para persistir y cargar el estado de la cola.
 * @property wordpressService Para obtener detalles completos de los episodios si es necesario.
 * @author Mario Alguacil Juárez
 */
class QueueViewModel(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService
) : ViewModel() {

    // IDs de los episodios en la cola.
    private val _queueEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val queueEpisodeIds: StateFlow<List<Int>> = _queueEpisodeIds.asStateFlow()

    // Objetos Episodio completos en la cola, para la UI.
    private val _queueEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val queueEpisodios: StateFlow<List<Episodio>> = _queueEpisodios.asStateFlow()

    // Caché de todos los episodios disponibles que se le pasa desde MainViewModel.
    // Ayuda a evitar llamadas innecesarias a la red si el episodio ya está en esta lista.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    init {
        loadQueueState()
    }

    /**
     * Establece la lista de todos los episodios disponibles (usualmente cargados al inicio de la app).
     * Esto permite al ViewModel construir la lista de `_queueEpisodios` más eficientemente.
     * @param episodes Lista completa de episodios disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Si la caché de episodios disponibles se actualiza, reconstruir la lista de objetos de la cola.
        viewModelScope.launch(Dispatchers.IO) {
            updateQueueEpisodiosListFromIds()
        }
    }

    /**
     * Carga los IDs de la cola desde SharedPreferences y luego actualiza la lista de objetos Episodio.
     */
    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = appPreferences.loadEpisodeQueue()
            Timber.d("IDs de cola cargados: ${_queueEpisodeIds.value}")
            updateQueueEpisodiosListFromIds()
        }
    }

    /**
     * Guarda la lista actual de IDs de la cola en SharedPreferences.
     */
    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) { // Asegurar que se guarda en un hilo de fondo
            appPreferences.saveEpisodeQueue(_queueEpisodeIds.value)
            Timber.d("IDs de cola guardados: ${_queueEpisodeIds.value}")
        }
    }

    /**
     * Actualiza `_queueEpisodios` (la lista de objetos Episodio) basada en `_queueEpisodeIds`.
     * Intenta obtener los detalles del episodio desde `allAvailableEpisodesCache` primero,
     * y si no se encuentra, lo busca a través de `wordpressService`.
     */
    private suspend fun updateQueueEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _queueEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                // Si no está en la caché, intentar obtenerlo del servicio.
                // Esto podría pasar si un ID se añadió a la cola pero la caché no estaba actualizada.
                try {
                    wordpressService.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    } ?: Timber.w("No se encontró el episodio con ID $id para la cola.")
                } catch (e: Exception) {
                    Timber.e(e, "Error al obtener detalles del episodio con ID $id para la cola.")
                }
            }
        }
        _queueEpisodios.value = episodeDetailsList
        Timber.d("Lista de episodios en cola actualizada con ${episodeDetailsList.size} items.")
    }

    /**
     * Añade un episodio a la cola de reproducción si no está ya presente.
     * @param episodio El episodio a añadir.
     */
    fun addEpisodeToQueue(episodio: Episodio) {
        if (!_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value + episodio.id
            saveQueueState() // Guardar la lista de IDs actualizada.
            // Actualizar la lista de objetos Episodio inmediatamente.
            _queueEpisodios.value = _queueEpisodios.value + episodio
            Timber.d("Episodio '${episodio.title}' (ID: ${episodio.id}) añadido a la cola. Tamaño nuevo: ${_queueEpisodios.value.size}")
        } else {
            Timber.d("Episodio '${episodio.title}' (ID: ${episodio.id}) ya está en la cola.")
        }
    }

    /**
     * Elimina un episodio de la cola de reproducción.
     * @param episodio El episodio a eliminar.
     */
    fun removeEpisodeFromQueue(episodio: Episodio) {
        if (_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value - episodio.id
            saveQueueState() // Guardar la lista de IDs actualizada.
            // Actualizar la lista de objetos Episodio.
            _queueEpisodios.value = _queueEpisodios.value.filterNot { it.id == episodio.id }
            Timber.d("Episodio '${episodio.title}' (ID: ${episodio.id}) eliminado de la cola.")
        }
    }

    /**
     * Elimina el episodio que se acaba de reproducir de la cola y devuelve el siguiente.
     * @param playedEpisodeId El ID del episodio que acaba de terminar.
     * @return El siguiente [Episodio] en la cola para reproducir, o null si la cola está vacía o el siguiente no se puede cargar.
     */
    suspend fun dequeueNextEpisode(playedEpisodeId: Int): Episodio? {
        val currentIds = _queueEpisodeIds.value.toMutableList()
        val originalQueueObjects = _queueEpisodios.value // Copia de la lista actual de objetos

        val playedIndex = currentIds.indexOf(playedEpisodeId)

        if (playedIndex != -1) {
            currentIds.removeAt(playedIndex)
            _queueEpisodeIds.value = currentIds // Actualizar el StateFlow de IDs
            saveQueueState() // Guardar el nuevo estado de IDs

            // Reconstruir la lista de objetos basada en los nuevos IDs y el orden
            val newQueueObjects = mutableListOf<Episodio>()
            for (id in currentIds) {
                originalQueueObjects.find { it.id == id }?.let { newQueueObjects.add(it) }
                    ?: wordpressService.getEpisodio(id)?.let { newQueueObjects.add(it) } // Obtener si no estaba en la lista de objetos original
            }
            _queueEpisodios.value = newQueueObjects // Actualizar el StateFlow de objetos

            Timber.d("Episodio ID $playedEpisodeId sacado de la cola. Tamaño nuevo de IDs: ${currentIds.size}")
            return newQueueObjects.firstOrNull() // Devuelve el primero de la nueva lista de objetos
        }

        // Si el playedEpisodeId no estaba en la cola de IDs (caso raro),
        // simplemente devuelve el primer elemento de la cola de objetos actual si existe.
        Timber.w("Episodio ID $playedEpisodeId no encontrado en la cola de IDs al intentar sacar.")
        return originalQueueObjects.firstOrNull()
    }

    /**
     * Limpia completamente la cola de reproducción.
     */
    fun clearQueue() {
        _queueEpisodeIds.value = emptyList()
        _queueEpisodios.value = emptyList()
        saveQueueState()
        Timber.d("Cola de reproducción limpiada.")
    }

    // TODO: Implementar reordenamiento de la cola
}