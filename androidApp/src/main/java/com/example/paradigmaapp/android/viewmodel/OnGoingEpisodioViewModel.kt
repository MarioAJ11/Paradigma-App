package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar los episodios cuya reproducción está en curso o ha sido pausada.
 * Esta versión es autosuficiente para el modo offline.
 *
 * @property appPreferences Instancia de [AppPreferences] para persistir y recuperar el progreso y los detalles del episodio.
 *
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodioViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _onGoingEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val onGoingEpisodios: StateFlow<List<Episodio>> = _onGoingEpisodios.asStateFlow()

    init {
        refrescarListaEpisodiosEnCurso()
    }

    /**
     * Carga/Refresca la lista de episodios en curso.
     * Obtiene todas las posiciones de reproducción guardadas y, para cada una
     * con progreso significativo (> 0), carga los detalles completos del episodio desde AppPreferences.
     */
    fun refrescarListaEpisodiosEnCurso() {
        viewModelScope.launch(Dispatchers.IO) {
            val episodePositions = appPreferences.getAllEpisodePositions()
            val episodiosEnCurso = mutableListOf<Episodio>()

            for ((idStr, position) in episodePositions) {
                if (position > 0) {
                    val episodioId = idStr.toIntOrNull()
                    if (episodioId != null) {
                        appPreferences.loadEpisodioDetails(episodioId)?.let { episodio ->
                            episodiosEnCurso.add(episodio)
                        }
                    }
                }
            }

            val episodiosOrdenados = episodiosEnCurso.sortedByDescending {
                appPreferences.getEpisodePosition(it.id)
            }

            withContext(Dispatchers.Main) {
                _onGoingEpisodios.value = episodiosOrdenados
            }
        }
    }

    /**
     * Guarda los detalles de un episodio y refresca la lista de "en curso".
     * Se llama cuando el progreso de un episodio cambia significativamente.
     *
     * @param episodio El episodio actualizado.
     */
    fun addOrUpdateOnGoingEpisode(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveEpisodioDetails(episodio)
            refrescarListaEpisodiosEnCurso() // Refresca la lista para reflejar cualquier cambio
        }
    }
}