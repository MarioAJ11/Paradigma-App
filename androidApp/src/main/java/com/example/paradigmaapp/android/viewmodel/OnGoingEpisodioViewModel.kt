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
 * ViewModel para gestionar los episodios en curso de reproducción.
 *
 * @property appPreferences Para obtener las posiciones guardadas de los episodios.
 * @property wordpressService Para obtener detalles de los episodios si es necesario.
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodioViewModel(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService // Necesario si los episodios no están todos cargados
) : ViewModel() {

    private val _onGoingEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val onGoingEpisodios: StateFlow<List<Episodio>> = _onGoingEpisodios.asStateFlow()

    // Mapa para el progreso activo: Key: episodeId, Value: Pair(currentPositionMillis, totalDurationMillis)
    private val _episodeProgressMap = MutableStateFlow<Map<Int, Pair<Long, Long>>>(emptyMap())

    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Cuando se establece la lista base, identificar los que están en progreso
        identifyOnGoingEpisodes()
    }

    private fun identifyOnGoingEpisodes() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentOnGoing = mutableListOf<Episodio>()
            val initialProgressMap = mutableMapOf<Int, Pair<Long, Long>>()

            for (episodio in allAvailableEpisodesCache) {
                val savedPosition = appPreferences.getEpisodePosition(episodio.id)
                // Asumimos que la duración en milisegundos la obtenemos del reproductor
                // Aquí, si no tenemos duración del episodio, no podemos calcular si "le quedan 10s"
                // Para simplificar, si hay posición guardada > 0, lo consideramos "en curso".
                // La lógica de "le quedan 10s" se aplicará cuando se actualice el progreso activamente.
                if (savedPosition > 0) {
                    // No podemos obtener la duración total aquí fácilmente sin el reproductor
                    // o si no está en el objeto Episodio.
                    // Podríamos añadir el episodio y el mapa de progreso se actualizará
                    // cuando el reproductor empiece a reportar duración.
                    currentOnGoing.add(episodio)
                    initialProgressMap[episodio.id] = Pair(savedPosition, 0L) // Duración desconocida inicialmente
                }
            }
            _episodeProgressMap.value = initialProgressMap
            _onGoingEpisodios.value = currentOnGoing.sortedByDescending {
                appPreferences.getEpisodePosition(it.id) // Ordenar por la posición más avanzada
            }
            Timber.d("Identified ${_onGoingEpisodios.value.size} on-going episodes initially.")
        }
    }

    /**
     * Actualiza el progreso de un episodio. Llamado por el reproductor.
     * @param episodeId ID del episodio.
     * @param currentPosition Posición actual en milisegundos.
     * @param totalDuration Duración total en milisegundos.
     */
    fun updateEpisodeProgress(episodeId: Int, currentPosition: Long, totalDuration: Long) {
        if (totalDuration <= 0) return // No se puede calcular el progreso sin duración

        val remainingTime = totalDuration - currentPosition
        val progressMap = _episodeProgressMap.value.toMutableMap()

        if (remainingTime > 10000) { // Más de 10 segundos restantes
            progressMap[episodeId] = Pair(currentPosition, totalDuration)
        } else {
            progressMap.remove(episodeId) // Casi terminado o terminado, quitar del progreso activo
        }
        _episodeProgressMap.value = progressMap
        refreshOnGoingList() // Actualizar la lista visible
    }

    /**
     * Notifica que un episodio se ha completado (posición reseteada a 0).
     */
    fun markEpisodeAsCompleted(episodeId: Int) {
        val progressMap = _episodeProgressMap.value.toMutableMap()
        progressMap.remove(episodeId)
        _episodeProgressMap.value = progressMap
        appPreferences.saveEpisodePosition(episodeId, 0L) // Asegurar que la posición guardada es 0
        refreshOnGoingList()
    }


    private fun refreshOnGoingList() {
        // Re-evaluar la lista _onGoingEpisodios basada en _episodeProgressMap y posiciones guardadas.
        val updatedOnGoing = mutableListOf<Episodio>()
        val currentProgressMap = _episodeProgressMap.value

        // Primero, los que están en el mapa de progreso activo
        for (episodeId in currentProgressMap.keys) {
            allAvailableEpisodesCache.find { it.id == episodeId }?.let {
                updatedOnGoing.add(it)
            }
        }

        // Luego, los que tienen posición guardada significativa y no están ya en el mapa de progreso activo
        for (episodio in allAvailableEpisodesCache) {
            if (!currentProgressMap.containsKey(episodio.id)) {
                val savedPosition = appPreferences.getEpisodePosition(episodio.id)
                // Aquí necesitaríamos la duración para la lógica de "más de 10s restantes"
                // Si la duración no está en Episodio.kt, esta lógica es difícil aquí.
                // Por ahora, si tiene posición guardada > 0, lo añadimos si no estaba ya.
                if (savedPosition > 0 && updatedOnGoing.none { it.id == episodio.id }) {
                    updatedOnGoing.add(episodio)
                }
            }
        }

        _onGoingEpisodios.value = updatedOnGoing.distinctBy { it.id }.sortedByDescending {
            currentProgressMap[it.id]?.first ?: appPreferences.getEpisodePosition(it.id)
        }
        Timber.d("Refreshed on-going episodes list: ${_onGoingEpisodios.value.size}")
    }


    fun getProgressForEpisode(episodeId: Int): Pair<Long, Long>? {
        val activeProgress = _episodeProgressMap.value[episodeId]
        if (activeProgress != null && activeProgress.second > 0) return activeProgress // (position, duration)

        // Si no está en progreso activo, o la duración es 0, intenta con la posición guardada
        val savedPosition = appPreferences.getEpisodePosition(episodeId)
        if (savedPosition > 0) {
            // No tenemos la duración aquí a menos que la cacheados de alguna forma
            // o el objeto Episodio la contenga (lo cual no hace actualmente desde la API).
            // Devolvemos (savedPosition, 0L) indicando duración desconocida.
            // El UI tendrá que manejar esto (e.g., mostrar solo progreso si la duración es 0).
            return Pair(savedPosition, _episodeProgressMap.value[episodeId]?.second ?: 0L)
        }
        return null
    }
}