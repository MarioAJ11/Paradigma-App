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
 * ViewModel para gestionar los episodios cuya reproducción está en curso o ha sido pausada.
 * Se encarga de:
 * - Obtener y guardar las posiciones de reproducción de estos episodios usando [AppPreferences].
 * - Cargar los detalles completos de los episodios a través del [EpisodioRepository] si no
 * se encuentran en la caché local (proporcionada por [MainViewModel]).
 * - Mantener una lista actualizada de episodios "en curso" para la UI.
 *
 * @property appPreferences Instancia de [AppPreferences] para persistir y recuperar el progreso.
 * @property episodioRepository Repositorio para obtener detalles de episodios si no están en caché.
 *
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodioViewModel(
    private val appPreferences: AppPreferences, //
    private val episodioRepository: EpisodioRepository //
) : ViewModel() {

    // Lista de objetos Episodio que están actualmente en curso (reproduciéndose o pausados).
    private val _onGoingEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val onGoingEpisodios: StateFlow<List<Episodio>> = _onGoingEpisodios.asStateFlow()

    // Mapa para el progreso activo de los episodios: Clave: episodeId, Valor: Pair(posiciónActualMillis, duraciónTotalMillis).
    // Usado para actualizaciones frecuentes mientras un episodio se está reproduciendo y para ordenar.
    private val _episodeProgressMap = MutableStateFlow<Map<Int, Pair<Long, Long>>>(emptyMap())
    // No se expone directamente si solo se usa internamente para ordenar y la UI obtiene progreso de otra forma.

    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    /**
     * Establece la lista de todos los episodios disponibles (cargados al inicio de la app por [MainViewModel])
     * y actualiza la lista de episodios "en curso" basándose en esta caché y las posiciones guardadas.
     *
     * @param episodes Lista completa de [Episodio]s disponibles en la aplicación.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        identifyOnGoingEpisodesFromCacheAndPrefs()
    }

    /**
     * Identifica los episodios que están "en curso" (no completados y con progreso guardado).
     * Actualiza `_onGoingEpisodios` y `_episodeProgressMap`.
     * Se ejecuta en un hilo de IO para leer de SharedPreferences.
     */
    private fun identifyOnGoingEpisodesFromCacheAndPrefs() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentOnGoingList = mutableListOf<Episodio>()
            val activeProgressMap = _episodeProgressMap.value.toMutableMap() // Usar copia del mapa actual

            // Itera sobre la caché de todos los episodios.
            for (episodio in allAvailableEpisodesCache) {
                val savedPosition = appPreferences.getEpisodePosition(episodio.id) //
                val progressInfo = activeProgressMap[episodio.id]

                // Considerar "en curso" si tiene una posición guardada significativa (>0) Y no está completado.
                // O si está en el mapa de progreso activo y no está completado.
                val isActiveInMap = progressInfo != null && progressInfo.second > 0 && (progressInfo.second - progressInfo.first > 10000) // Más de 10s restantes
                val hasSignificantSavedPosition = savedPosition > 0 // Consideramos 0 como completado o no iniciado.

                if (isActiveInMap || hasSignificantSavedPosition) {
                    if (!currentOnGoingList.any { it.id == episodio.id }) {
                        currentOnGoingList.add(episodio)
                    }
                    // Actualizar o añadir al mapa de progreso si tiene posición guardada y no estaba.
                    // La duración total (second) podría ser 0L si solo tenemos la posición guardada y no la duración.
                    if (hasSignificantSavedPosition && !activeProgressMap.containsKey(episodio.id)) {
                        activeProgressMap[episodio.id] = Pair(savedPosition, progressInfo?.second ?: 0L)
                    }
                } else {
                    // Si no tiene posición guardada significativa y no está activo en el mapa,
                    // se asegura de quitarlo del mapa de progreso.
                    activeProgressMap.remove(episodio.id)
                }
            }
            _episodeProgressMap.value = activeProgressMap
            _onGoingEpisodios.value = currentOnGoingList.distinctBy { it.id }
                .sortedByDescending { ep -> // Ordenar por la posición más reciente/mayor progreso
                    _episodeProgressMap.value[ep.id]?.first ?: appPreferences.getEpisodePosition(ep.id)
                }
        }
    }

    /**
     * Actualiza el progreso de reproducción de un episodio.
     * Esta función es llamada por [MainViewModel] durante la reproducción del audio.
     * Si el episodio está casi terminado, se elimina de la lista de "en curso".
     *
     * @param episodeId ID del episodio.
     * @param currentPosition Posición actual de reproducción en milisegundos.
     * @param totalDuration Duración total del episodio en milisegundos.
     */
    fun updateEpisodeProgress(episodeId: Int, currentPosition: Long, totalDuration: Long) {
        if (totalDuration <= 0) return // No se puede calcular el progreso sin duración total.

        val progressMap = _episodeProgressMap.value.toMutableMap()
        val episodio = allAvailableEpisodesCache.find { it.id == episodeId }

        val remainingTime = totalDuration - currentPosition
        // Considerar "en curso" si quedan más de, por ejemplo, 10 segundos.
        if (remainingTime > 10000) {
            progressMap[episodeId] = Pair(currentPosition, totalDuration)
            if (episodio != null && _onGoingEpisodios.value.none { it.id == episodeId }) {
                _onGoingEpisodios.value = (_onGoingEpisodios.value + episodio)
                    .distinctBy { it.id }
                    .sortedByDescending { ep ->
                        progressMap[ep.id]?.first ?: appPreferences.getEpisodePosition(ep.id)
                    }
            } else {
                // Re-ordenar si ya estaba en la lista pero su progreso cambió significativamente.
                _onGoingEpisodios.value = _onGoingEpisodios.value
                    .sortedByDescending { ep ->
                        progressMap[ep.id]?.first ?: appPreferences.getEpisodePosition(ep.id)
                    }
            }
        } else {
            // Si queda poco tiempo o se completó, se elimina del progreso activo y de la lista "en curso".
            progressMap.remove(episodeId)
            appPreferences.saveEpisodePosition(episodeId, 0L) // Marcar como completado en prefs
            _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
        }
        _episodeProgressMap.value = progressMap
    }

    /**
     * Marca un episodio como completado, eliminándolo de la lista de "en curso"
     * y del mapa de progreso activo.
     *
     * @param episodeId El ID del episodio que se ha completado.
     */
    fun markEpisodeAsCompleted(episodeId: Int) {
        val progressMap = _episodeProgressMap.value.toMutableMap()
        progressMap.remove(episodeId) // Quitar del mapa de progreso activo.
        _episodeProgressMap.value = progressMap

        // MainViewModel ya guarda la posición como 0L en SharedPreferences.
        _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
    }

    /**
     * Obtiene el progreso (posición actual y duración total) para un episodio específico.
     * Útil para la UI, por ejemplo, para mostrar una barra de progreso en un ítem de lista.
     *
     * @param episodeId El ID del episodio.
     * @return Un `Pair<Long, Long>` con (posiciónActualMillis, duraciónTotalMillis),
     * o `null` si no hay información de progreso. La duración total puede ser 0L si aún no se conoce.
     */
    fun getProgressForEpisode(episodeId: Int): Pair<Long, Long>? {
        val activeProgress = _episodeProgressMap.value[episodeId]
        if (activeProgress != null) {
            return activeProgress
        }
        // Si no está en progreso activo, intentar con la posición guardada.
        val savedPosition = appPreferences.getEpisodePosition(episodeId)
        // Devolver solo si hay una posición guardada significativa (no completado)
        return if (savedPosition > 0) {
            // Duración total (0L) es desconocida si solo se tiene la posición guardada.
            Pair(savedPosition, 0L)
        } else {
            null
        }
    }
}