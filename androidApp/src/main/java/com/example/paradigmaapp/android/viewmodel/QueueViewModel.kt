package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel para gestionar la cola de reproducción.
 *
 * @property appPreferences Para persistir el estado de la cola.
 * @property wordpressService Para obtener detalles de los episodios.
 * @author Mario Alguacil Juárez
 */
class QueueViewModel(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService
) : ViewModel() {

    private val _queueEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val queueEpisodeIds: StateFlow<List<Int>> = _queueEpisodeIds.asStateFlow()

    private val _queueEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val queueEpisodios: StateFlow<List<Episodio>> = _queueEpisodios.asStateFlow()

    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    init {
        loadQueueState()
    }

    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Volver a cargar la lista de objetos de la cola si los episodios base cambian
        viewModelScope.launch(Dispatchers.IO) {
            updateQueueEpisodiosListFromIds()
        }
    }

    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = appPreferences.loadEpisodeQueue()
            Timber.d("Loaded episode queue IDs: ${_queueEpisodeIds.value}")
            updateQueueEpisodiosListFromIds()
        }
    }

    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveEpisodeQueue(_queueEpisodeIds.value)
            Timber.d("Saved episode queue IDs: ${_queueEpisodeIds.value}")
        }
    }

    private suspend fun updateQueueEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _queueEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                try {
                    wordpressService.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch details for episode ID $id in queue")
                }
            }
        }
        _queueEpisodios.value = episodeDetailsList
        Timber.d("Updated queue episodios list with ${episodeDetailsList.size} items.")
    }

    fun addEpisodeToQueue(episodio: Episodio) {
        if (!_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value + episodio.id
            saveQueueState()
            // Actualizar la lista de objetos inmediatamente si tenemos el objeto
            _queueEpisodios.value = _queueEpisodios.value + episodio
            Timber.d("Added '${episodio.title}' to queue. New queue size: ${_queueEpisodios.value.size}")
        } else {
            Timber.d("Episodio '${episodio.title}' is already in queue.")
        }
    }

    fun removeEpisodeFromQueue(episodio: Episodio) {
        if (_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value - episodio.id
            saveQueueState()
            _queueEpisodios.value = _queueEpisodios.value.filterNot { it.id == episodio.id }
            Timber.d("Removed '${episodio.title}' from queue.")
        }
    }

    /**
     * Elimina el episodio actual de la cabeza de la cola y devuelve el siguiente.
     * @param playedEpisodeId El ID del episodio que acaba de terminar.
     * @return El siguiente [Episodio] en la cola, o null si la cola está vacía o el siguiente no se puede cargar.
     */
    suspend fun dequeueNextEpisode(playedEpisodeId: Int): Episodio? {
        val currentIds = _queueEpisodeIds.value.toMutableList()
        val playedIndex = currentIds.indexOf(playedEpisodeId)

        if (playedIndex != -1) {
            currentIds.removeAt(playedIndex) // Elimina el que se reprodujo
            if (playedIndex < currentIds.size) { // Si había uno después del que se eliminó en esa posición
                _queueEpisodeIds.value = currentIds
                saveQueueState()
                val nextEpisodeId = currentIds[playedIndex] // El siguiente es el que ahora ocupa esa posición
                updateQueueEpisodiosListFromIds() // Actualiza la lista de objetos
                return _queueEpisodios.value.find { it.id == nextEpisodeId } ?: wordpressService.getEpisodio(nextEpisodeId)
            } else if (currentIds.isNotEmpty() && playedIndex == 0) { // Si se eliminó el primero y aún quedan
                _queueEpisodeIds.value = currentIds
                saveQueueState()
                val nextEpisodeId = currentIds.first()
                updateQueueEpisodiosListFromIds()
                return _queueEpisodios.value.find { it.id == nextEpisodeId } ?: wordpressService.getEpisodio(nextEpisodeId)
            } else { // La cola está vacía o se eliminó el último
                _queueEpisodeIds.value = currentIds
                saveQueueState()
                updateQueueEpisodiosListFromIds()
                return null
            }
        }
        // Si el playedEpisodeId no estaba en la cola (caso raro, pero seguro)
        updateQueueEpisodiosListFromIds() // Sincroniza por si acaso
        return _queueEpisodios.value.firstOrNull() // Devuelve el primero de la cola actual
    }

    fun clearQueue() {
        _queueEpisodeIds.value = emptyList()
        _queueEpisodios.value = emptyList()
        saveQueueState()
        Timber.d("Queue cleared.")
    }

    // TODO: Implementar reorderQueue si es necesario, actualizando _queueEpisodeIds,
    // guardando, y luego llamando a updateQueueEpisodiosListFromIds.
}