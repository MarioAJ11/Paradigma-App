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
 * ViewModel para gestionar los episodios cuya reproducción está en curso.
 *
 * @property appPreferences Para obtener y guardar las posiciones de reproducción de los episodios.
 * @property wordpressService Para obtener detalles de los episodios si no están en la caché local.
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodioViewModel(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService
) : ViewModel() {

    // Lista de objetos Episodio que están en curso.
    private val _onGoingEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val onGoingEpisodios: StateFlow<List<Episodio>> = _onGoingEpisodios.asStateFlow()

    // Mapa para el progreso activo: Key: episodeId, Value: Pair(currentPositionMillis, totalDurationMillis)
    // Se usa para actualizaciones frecuentes mientras se reproduce.
    private val _episodeProgressMap = MutableStateFlow<Map<Int, Pair<Long, Long>>>(emptyMap())

    // Caché de todos los episodios disponibles, pasada desde MainViewModel.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    /**
     * Establece la lista de todos los episodios disponibles y (re)identifica los que están en curso.
     * @param episodes Lista completa de episodios disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        identifyOnGoingEpisodesFromCacheAndPrefs()
    }

    /**
     * Identifica los episodios en curso basándose en la caché de episodios disponibles
     * y las posiciones guardadas en SharedPreferences.
     */
    private fun identifyOnGoingEpisodesFromCacheAndPrefs() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentOnGoingList = mutableListOf<Episodio>()
            val activeProgressMap = _episodeProgressMap.value.toMutableMap() // Usar el mapa de progreso actual

            for (episodio in allAvailableEpisodesCache) {
                val savedPosition = appPreferences.getEpisodePosition(episodio.id)
                val progressInfo = activeProgressMap[episodio.id]

                // Un episodio se considera "en curso" si:
                // 1. Está en el mapa de progreso activo (se está reproduciendo o pausó recientemente) Y le queda tiempo.
                // 2. O tiene una posición guardada significativa (> 0) Y no está marcado como completado (posición 0).
                val isActiveInMap = progressInfo != null && progressInfo.second > 0 && (progressInfo.second - progressInfo.first > 10000)
                val hasSignificantSavedPosition = savedPosition > 0

                if (isActiveInMap) {
                    if (!currentOnGoingList.any { it.id == episodio.id }) {
                        currentOnGoingList.add(episodio)
                    }
                } else if (hasSignificantSavedPosition) {
                    // Si no está en el mapa activo pero tiene posición guardada, añadirlo
                    // y tratar de poner una entrada en el mapa de progreso si no existe.
                    if (!currentOnGoingList.any { it.id == episodio.id }) {
                        currentOnGoingList.add(episodio)
                    }
                    if (!activeProgressMap.containsKey(episodio.id)) {
                        // No tenemos la duración total aquí, así que la ponemos a 0
                        // Se actualizará cuando el episodio se reproduzca y el reproductor informe la duración.
                        activeProgressMap[episodio.id] = Pair(savedPosition, 0L)
                    }
                } else {
                    // Si no tiene posición guardada significativa y no está en el mapa activo, quitarlo del mapa.
                    activeProgressMap.remove(episodio.id)
                }
            }
            _episodeProgressMap.value = activeProgressMap // Actualizar el mapa de progreso
            _onGoingEpisodios.value = currentOnGoingList.distinctBy { it.id }.sortedByDescending {
                // Ordenar por la posición actual si está en el mapa, sino por la guardada.
                _episodeProgressMap.value[it.id]?.first ?: appPreferences.getEpisodePosition(it.id)
            }
            Timber.d("Identificados ${_onGoingEpisodios.value.size} episodios en curso.")
        }
    }

    /**
     * Actualiza el progreso de un episodio. Llamado por `MainViewModel` durante la reproducción.
     * @param episodeId ID del episodio.
     * @param currentPosition Posición actual en milisegundos.
     * @param totalDuration Duración total en milisegundos.
     */
    fun updateEpisodeProgress(episodeId: Int, currentPosition: Long, totalDuration: Long) {
        if (totalDuration <= 0) return // No se puede calcular el progreso sin duración

        val progressMap = _episodeProgressMap.value.toMutableMap()
        val episodio = allAvailableEpisodesCache.find{ it.id == episodeId }

        if (totalDuration - currentPosition > 10000) { // Más de 10 segundos restantes
            progressMap[episodeId] = Pair(currentPosition, totalDuration)
            // Asegurarse de que el episodio está en la lista visible si ahora califica como "en curso"
            if (episodio != null && _onGoingEpisodios.value.none { it.id == episodeId }) {
                _onGoingEpisodios.value = (_onGoingEpisodios.value + episodio).distinctBy { it.id }
            }
        } else {
            // Considerar casi terminado, remover del progreso activo y de la lista visible.
            progressMap.remove(episodeId)
            _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
        }
        _episodeProgressMap.value = progressMap
        // Reordenar después de actualizar el progreso
        _onGoingEpisodios.value = _onGoingEpisodios.value.sortedByDescending {
            _episodeProgressMap.value[it.id]?.first ?: appPreferences.getEpisodePosition(it.id)
        }
        // No es necesario guardar en SharedPreferences aquí, MainViewModel lo hace al pausar/finalizar.
    }

    /**
     * Marca un episodio como completado (o si su posición de guardado es 0).
     * Esto lo elimina de la lista de "en curso".
     * @param episodeId El ID del episodio completado.
     */
    fun markEpisodeAsCompleted(episodeId: Int) {
        val progressMap = _episodeProgressMap.value.toMutableMap()
        progressMap.remove(episodeId)
        _episodeProgressMap.value = progressMap
        // La posición ya debería haber sido guardada como 0 por MainViewModel
        _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
        Timber.d("Episodio ID $episodeId marcado como completado y eliminado de 'en curso'.")
    }


    /**
     * Obtiene el progreso (posición actual y duración total) para un episodio específico.
     * Útil para la UI, por ejemplo, para mostrar una barra de progreso en `EpisodioListItem`.
     * @param episodeId El ID del episodio.
     * @return Un `Pair<Long, Long>` con (posiciónActual, duraciónTotal), o null si no hay información.
     * La duración total puede ser 0L si aún no se conoce.
     */
    fun getProgressForEpisode(episodeId: Int): Pair<Long, Long>? {
        val activeProgress = _episodeProgressMap.value[episodeId]
        if (activeProgress != null) { // Si está en el mapa de progreso activo (reproduciendo o pausado recientemente)
            return activeProgress // Devuelve (posición, duración)
        }

        // Si no está en progreso activo, intenta con la posición guardada.
        val savedPosition = appPreferences.getEpisodePosition(episodeId)
        if (savedPosition > 0) {
            // No tenemos la duración total aquí si solo tenemos la posición guardada.
            // Devolvemos (savedPosition, 0L) para indicar duración desconocida.
            // La UI debería poder manejar una duración de 0L (ej. mostrar solo progreso como porcentaje si es posible, o solo la posición).
            return Pair(savedPosition, 0L)
        }
        return null // No hay información de progreso.
    }
}