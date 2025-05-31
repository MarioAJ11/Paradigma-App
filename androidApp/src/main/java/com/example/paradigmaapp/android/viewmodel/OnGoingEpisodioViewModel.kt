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
 * ViewModel para gestionar los episodios cuya reproducción está actualmente en curso o ha sido pausada.
 * Me encargo de obtener y guardar las posiciones de reproducción de estos episodios usando [AppPreferences],
 * y de cargar los detalles completos de los episodios a través del [EpisodioRepository] si no
 * se encuentran en la caché local que me proporciona el MainViewModel.
 * El objetivo es permitir al usuario continuar escuchando desde donde lo dejó.
 *
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodioViewModel(
    private val appPreferences: AppPreferences,
    // Ahora dependo de la abstracción del repositorio de episodios.
    private val episodioRepository: EpisodioRepository
) : ViewModel() {

    // Lista de objetos Episodio que están actualmente en curso (reproduciéndose o pausados).
    private val _onGoingEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val onGoingEpisodios: StateFlow<List<Episodio>> = _onGoingEpisodios.asStateFlow()

    // Mapa para el progreso activo de los episodios: Clave: episodeId, Valor: Pair(posiciónActualMillis, duraciónTotalMillis).
    // Lo uso para actualizaciones frecuentes mientras un episodio se está reproduciendo.
    private val _episodeProgressMap = MutableStateFlow<Map<Int, Pair<Long, Long>>>(emptyMap())

    // Caché de todos los episodios disponibles; esta lista me la pasa el MainViewModel.
    // Ayuda a evitar llamadas innecesarias a la red si el episodio ya está en esta caché.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()

    /**
     * Establece la lista de todos los episodios disponibles (cargados al inicio de la app)
     * y luego identifica (o re-identifica) cuáles de ellos están "en curso" basándose
     * en esta caché y en las posiciones guardadas en SharedPreferences.
     * @param episodes Lista completa de episodios disponibles en la aplicación.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        identifyOnGoingEpisodesFromCacheAndPrefs()
    }

    // Identifica los episodios que están "en curso"
    // Se basa en la caché de episodios y las posiciones guardadas en SharedPreferences.
    private fun identifyOnGoingEpisodesFromCacheAndPrefs() {
        viewModelScope.launch(Dispatchers.IO) { // Operación de lectura de SharedPreferences en hilo de IO.
            val currentOnGoingList = mutableListOf<Episodio>()
            val activeProgressMap = _episodeProgressMap.value.toMutableMap() // Uso el mapa de progreso actual.

            for (episodio in allAvailableEpisodesCache) {
                val savedPosition = appPreferences.getEpisodePosition(episodio.id)
                val progressInfo = activeProgressMap[episodio.id]

                // Un episodio se considera "en curso" si:
                // 1. Está en el mapa de progreso activo (se está reproduciendo o pausó recientemente) Y le queda tiempo significativo.
                // 2. O tiene una posición guardada significativa (> 0) y no está marcado como completado (posición 0).
                val isActiveInMap = progressInfo != null && progressInfo.second > 0 && (progressInfo.second - progressInfo.first > 10000) // Más de 10s restantes
                val hasSignificantSavedPosition = savedPosition > 0

                if (isActiveInMap) {
                    // Si está activo en el mapa de progreso, me aseguro de que esté en la lista visible.
                    if (!currentOnGoingList.any { it.id == episodio.id }) {
                        currentOnGoingList.add(episodio)
                    }
                } else if (hasSignificantSavedPosition) {
                    // Si no está activo en el mapa pero tiene una posición guardada, lo añado.
                    if (!currentOnGoingList.any { it.id == episodio.id }) {
                        currentOnGoingList.add(episodio)
                    }
                    // Y si no estaba en el mapa de progreso activo, añado una entrada.
                    // La duración total podría no conocerse aquí, se actualizará cuando se reproduzca.
                    if (!activeProgressMap.containsKey(episodio.id)) {
                        activeProgressMap[episodio.id] = Pair(savedPosition, 0L) // Duración total desconocida (0L)
                    }
                } else {
                    // Si no tiene posición guardada significativa y no está en el mapa activo, lo quito del mapa.
                    activeProgressMap.remove(episodio.id)
                }
            }
            _episodeProgressMap.value = activeProgressMap // Actualizo el mapa de progreso.
            // Actualizo la lista de episodios en curso, eliminando duplicados y ordenando.
            // Los ordeno para que los más recientemente escuchados (o con mayor progreso) aparezcan primero.
            _onGoingEpisodios.value = currentOnGoingList.distinctBy { it.id }.sortedByDescending {
                _episodeProgressMap.value[it.id]?.first ?: appPreferences.getEpisodePosition(it.id)
            }
            // Log eliminado: Timber.d("Identificados ${_onGoingEpisodios.value.size} episodios en curso.")
        }
    }

    /**
     * Actualiza el progreso de reproducción de un episodio.
     * Esta función es llamada por `MainViewModel` durante la reproducción del audio.
     * @param episodeId ID del episodio.
     * @param currentPosition Posición actual de reproducción en milisegundos.
     * @param totalDuration Duración total del episodio en milisegundos.
     */
    fun updateEpisodeProgress(episodeId: Int, currentPosition: Long, totalDuration: Long) {
        if (totalDuration <= 0) return // No puedo calcular el progreso si no tengo la duración total.

        val progressMap = _episodeProgressMap.value.toMutableMap()
        val episodio = allAvailableEpisodesCache.find{ it.id == episodeId }

        if (totalDuration - currentPosition > 10000) { // Si quedan más de 10 segundos.
            progressMap[episodeId] = Pair(currentPosition, totalDuration)
            // Me aseguro de que el episodio esté en la lista visible si ahora califica como "en curso".
            if (episodio != null && _onGoingEpisodios.value.none { it.id == episodeId }) {
                _onGoingEpisodios.value = (_onGoingEpisodios.value + episodio).distinctBy { it.id }
            }
        } else {
            // Si queda muy poco, lo considero casi terminado y lo elimino del progreso activo y de la lista visible.
            progressMap.remove(episodeId)
            _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
        }
        _episodeProgressMap.value = progressMap
        // Reordeno la lista después de actualizar el progreso.
        _onGoingEpisodios.value = _onGoingEpisodios.value.sortedByDescending {
            _episodeProgressMap.value[it.id]?.first ?: appPreferences.getEpisodePosition(it.id)
        }
        // La posición se guarda en SharedPreferences en MainViewModel al pausar o finalizar.
    }

    /**
     * Marca un episodio como completado (o si su posición guardada es 0).
     * Esto lo elimina de la lista de "en curso".
     * @param episodeId El ID del episodio que se ha completado.
     */
    fun markEpisodeAsCompleted(episodeId: Int) {
        val progressMap = _episodeProgressMap.value.toMutableMap()
        progressMap.remove(episodeId) // Lo quito del mapa de progreso activo.
        _episodeProgressMap.value = progressMap
        // MainViewModel ya debería haber guardado la posición como 0 en SharedPreferences.
        // Lo elimino de la lista visible de episodios en curso.
        _onGoingEpisodios.value = _onGoingEpisodios.value.filterNot { it.id == episodeId }
        // Log eliminado: Timber.d("Episodio ID $episodeId marcado como completado y eliminado de 'en curso'.")
    }

    /**
     * Obtiene el progreso (posición actual y duración total) para un episodio específico.
     * Es útil para la UI, por ejemplo, para mostrar una barra de progreso en un `EpisodioListItem`.
     * @param episodeId El ID del episodio.
     * @return Un `Pair<Long, Long>` con (posiciónActualMillis, duraciónTotalMillis),
     * o `null` si no hay información de progreso. La duración total puede ser 0L si aún no se conoce.
     */
    fun getProgressForEpisode(episodeId: Int): Pair<Long, Long>? {
        val activeProgress = _episodeProgressMap.value[episodeId]
        if (activeProgress != null) { // Si está en el mapa de progreso activo (reproduciéndose o pausado recientemente).
            return activeProgress
        }

        // Si no está en progreso activo, intento con la posición guardada en SharedPreferences.
        val savedPosition = appPreferences.getEpisodePosition(episodeId)
        if (savedPosition > 0) {
            // Si solo tengo la posición guardada, no conozco la duración total aquí.
            // Devuelvo (savedPosition, 0L) para indicar duración desconocida.
            // La UI debería ser capaz de manejar una duración de 0L (ej. no mostrando el total o la barra como indeterminada).
            return Pair(savedPosition, 0L)
        }
        return null // No hay información de progreso para este episodio.
    }
}