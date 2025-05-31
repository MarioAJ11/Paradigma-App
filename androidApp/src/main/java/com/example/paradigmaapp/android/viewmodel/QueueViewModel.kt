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

/**
 * ViewModel para gestionar mi cola de reproducción de episodios.
 * Se encarga de cargar y guardar el estado de la cola usando [AppPreferences],
 * y de obtener los detalles completos de los episodios (si es necesario) a través
 * del [EpisodioRepository] para mostrarlos en la UI.
 *
 * @author Mario Alguacil Juárez
 */
class QueueViewModel(
    private val appPreferences: AppPreferences,
    // Ahora dependo de la abstracción del repositorio.
    private val episodioRepository: EpisodioRepository
) : ViewModel() {

    // Contiene los IDs de los episodios en la cola de reproducción.
    private val _queueEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val queueEpisodeIds: StateFlow<List<Int>> = _queueEpisodeIds.asStateFlow()

    // Contiene los objetos Episodio completos de la cola, para la UI.
    private val _queueEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val queueEpisodios: StateFlow<List<Episodio>> = _queueEpisodios.asStateFlow()

    // Caché de todos los episodios disponibles que me pasa MainViewModel.
    // Ayuda a evitar llamadas innecesarias a la red si el episodio ya está en esta lista.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    init {
        loadQueueState() // Cargo el estado de la cola al iniciar.
    }

    /**
     * Establece la lista de todos los episodios disponibles (cargados al inicio de la app).
     * Esto me permite construir la lista de `_queueEpisodios` de forma más eficiente.
     * @param episodes Lista completa de episodios disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Si la caché de episodios se actualiza, reconstruyo la lista de objetos de la cola.
        viewModelScope.launch(Dispatchers.IO) {
            updateQueueEpisodiosListFromIds()
        }
    }

    // Carga los IDs de la cola desde SharedPreferences y luego actualiza la lista de objetos Episodio.
    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = appPreferences.loadEpisodeQueue()
            updateQueueEpisodiosListFromIds()
        }
    }

    // Guarda la lista actual de IDs de la cola en SharedPreferences.
    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) { // Aseguro que se guarda en un hilo de fondo.
            appPreferences.saveEpisodeQueue(_queueEpisodeIds.value)
        }
    }

    // Actualiza `_queueEpisodios` (la lista de objetos Episodio) basada en `_queueEpisodeIds`.
    // Intenta obtener los detalles del episodio desde `allAvailableEpisodesCache` primero,
    // y si no se encuentra, lo busca a través de `episodioRepository`.
    private suspend fun updateQueueEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _queueEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                // Si no está en la caché, intento obtenerlo del servicio (ahora repositorio).
                try {
                    // Uso la interfaz del repositorio.
                    episodioRepository.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    }
                } catch (e: Exception) {
                }
            }
        }
        _queueEpisodios.value = episodeDetailsList
    }

    /**
     * Añade un episodio a la cola de reproducción si no está ya presente.
     * @param episodio El episodio a añadir.
     */
    fun addEpisodeToQueue(episodio: Episodio) {
        if (!_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value + episodio.id
            saveQueueState() // Guardo la lista de IDs actualizada.
            _queueEpisodios.value = _queueEpisodios.value + episodio
        }
    }

    /**
     * Elimina un episodio de la cola de reproducción.
     * @param episodio El episodio a eliminar.
     */
    fun removeEpisodeFromQueue(episodio: Episodio) {
        if (_queueEpisodeIds.value.contains(episodio.id)) {
            _queueEpisodeIds.value = _queueEpisodeIds.value - episodio.id
            saveQueueState() // Guardo la lista de IDs actualizada.
            // Actualizo la lista de objetos Episodio.
            _queueEpisodios.value = _queueEpisodios.value.filterNot { it.id == episodio.id }
        }
    }

    /**
     * Elimina el episodio que se acaba de reproducir de la cola y devuelve el siguiente.
     * @param playedEpisodeId El ID del episodio que acaba de terminar.
     * @return El siguiente [Episodio] en la cola para reproducir, o null si la cola está vacía
     * o el siguiente no se puede cargar.
     */
    suspend fun dequeueNextEpisode(playedEpisodeId: Int): Episodio? {
        val currentIds = _queueEpisodeIds.value.toMutableList()
        val originalQueueObjects = _queueEpisodios.value // Copia de la lista actual de objetos.

        val playedIndex = currentIds.indexOf(playedEpisodeId)

        if (playedIndex != -1) {
            currentIds.removeAt(playedIndex)
            _queueEpisodeIds.value = currentIds // Actualizo el StateFlow de IDs.
            saveQueueState() // Guardo el nuevo estado de IDs.

            // Reconstruyo la lista de objetos basada en los nuevos IDs y el orden.
            val newQueueObjects = mutableListOf<Episodio>()
            for (id in currentIds) {
                originalQueueObjects.find { it.id == id }?.let { newQueueObjects.add(it) }
                    ?: episodioRepository.getEpisodio(id)?.let { newQueueObjects.add(it) } // Obtener si no estaba en la lista de objetos original.
            }
            _queueEpisodios.value = newQueueObjects // Actualizo el StateFlow de objetos.

            return newQueueObjects.firstOrNull() // Devuelvo el primero de la nueva lista de objetos.
        }

        // Si el playedEpisodeId no estaba en la cola de IDs (caso raro),
        // simplemente devuelvo el primer elemento de la cola de objetos actual si existe.
        return originalQueueObjects.firstOrNull()
    }

    /**
     * Limpia completamente la cola de reproducción.
     */
    fun clearQueue() {
        _queueEpisodeIds.value = emptyList()
        _queueEpisodios.value = emptyList()
        saveQueueState()
    }
}