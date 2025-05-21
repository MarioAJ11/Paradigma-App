package com.example.paradigmaapp.android.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.paradigmaapp.android.podcast.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class OnGoingPodcastViewModel(
    private val getPodcastPosition: (podcastUrl: String) -> Long
) : ViewModel() {

    private val _allPodcasts = mutableStateListOf<Podcast>()

    // Un mapa para almacenar el progreso actual de los podcasts en curso
    // Key: URL del podcast, Value: Pair(currentPositionMillis, totalDurationMillis)
    private val _podcastProgressMap = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())

    // StateFlow de los podcasts "en progreso"
    private val _onGoingPodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val onGoingPodcasts: StateFlow<List<Podcast>> = _onGoingPodcasts.asStateFlow()

    fun setAllPodcasts(podcasts: List<Podcast>) {
        _allPodcasts.clear()
        _allPodcasts.addAll(podcasts)
        // Al iniciar, verifica las posiciones guardadas para identificar podcasts en curso
        // y añade su progreso inicial al mapa.
        val initialProgresses = mutableMapOf<String, Pair<Long, Long>>()
        for (podcast in _allPodcasts) {
            val savedPosition = getPodcastPosition(podcast.url)
            val durationMillis = podcast.duration.toLongOrNull()?.times(1000) ?: 0L

            // Un podcast se considera "en progreso" si tiene una posición guardada > 0
            // y aún le quedan al menos 10 segundos de reproducción.
            if (savedPosition > 0 && durationMillis > 0 && (durationMillis - savedPosition) > 10000) {
                initialProgresses[podcast.url] = Pair(savedPosition, durationMillis)
            }
        }
        _podcastProgressMap.value = initialProgresses // Inicializa el mapa con los podcasts guardados
        updateOnGoingPodcasts() // Actualiza la lista visible
    }

    // Llamado periódicamente por AppScreen para actualizar el progreso
    fun updatePodcastProgress(podcastUrl: String, currentPosition: Long, totalDuration: Long) {
        if (totalDuration > 0) {
            // Un podcast se considera "en progreso" si está en reproducción y le quedan más de 10 segundos
            // O si está pausado y tiene una posición guardada significativa.
            if (totalDuration - currentPosition > 10000) { // Todavía le quedan más de 10 segundos
                _podcastProgressMap.value = _podcastProgressMap.value.toMutableMap().also {
                    it[podcastUrl] = Pair(currentPosition, totalDuration)
                }
            } else {
                // Si el podcast está al principio o casi terminado, lo quitamos del progreso activo
                _podcastProgressMap.value = _podcastProgressMap.value.toMutableMap().also {
                    it.remove(podcastUrl)
                }
            }
            updateOnGoingPodcasts()
        }
    }

    // Actualiza la lista _onGoingPodcasts basándose en _allPodcasts y _podcastProgressMap
    private fun updateOnGoingPodcasts() {
        val currentOnGoing = mutableListOf<Podcast>()
        for (podcast in _allPodcasts) {
            val progressInfo = _podcastProgressMap.value[podcast.url]
            if (progressInfo != null) {
                // Está en el mapa de progreso, lo que significa que está en curso (sea por reproducción actual o por posición guardada significativa)
                currentOnGoing.add(podcast)
            } else {
                // Si no está en _podcastProgressMap, pero tiene una posición guardada significativa, también es "en curso"
                val savedPosition = getPodcastPosition(podcast.url)
                val durationMillis = podcast.duration.toLongOrNull()?.times(1000) ?: 0L
                if (savedPosition > 0 && durationMillis > 0 && (durationMillis - savedPosition) > 10000) {
                    currentOnGoing.add(podcast)
                }
            }
        }
        // Ordena los podcasts por el progreso (los que están más avanzados primero) o por título
        _onGoingPodcasts.value = currentOnGoing.sortedByDescending { podcast ->
            _podcastProgressMap.value[podcast.url]?.first ?: getPodcastPosition(podcast.url)
        }.distinctBy { it.identifier } // Elimina duplicados si por alguna razón aparecen
        Timber.d("Updated on-going podcasts: ${_onGoingPodcasts.value.size}")
    }

    // Función para obtener el progreso de un podcast específico (para el UI)
    fun getProgressForPodcast(podcastUrl: String): Pair<Long, Long>? {
        // Primero busca en el mapa de progreso activo
        val progressInfo = _podcastProgressMap.value[podcastUrl]
        if (progressInfo != null) {
            return progressInfo
        }

        // Si no está en progreso activo, busca en la posición guardada
        val podcast = _allPodcasts.find { it.url == podcastUrl }
        if (podcast != null) {
            val savedPosition = getPodcastPosition(podcastUrl)
            val durationMillis = podcast.duration.toLongOrNull()?.times(1000) ?: 0L
            if (savedPosition > 0 && durationMillis > 0 && (durationMillis - savedPosition) > 10000) {
                return Pair(savedPosition, durationMillis)
            }
        }
        return null
    }
}