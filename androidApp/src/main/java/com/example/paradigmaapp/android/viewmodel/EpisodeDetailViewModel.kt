package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa // Necesario para obtener nombres de programas
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class EpisodeDetailViewModel(
    private val wordpressService: WordpressService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val episodeId: Int = savedStateHandle.get<Int>("episodeId") ?: -1

    private val _episodio = MutableStateFlow<Episodio?>(null)
    val episodio: StateFlow<Episodio?> = _episodio.asStateFlow()

    // Para mostrar los nombres de los programas asociados ("Creadores")
    private val _programasAsociados = MutableStateFlow<List<Programa>>(emptyList())
    val programasAsociados: StateFlow<List<Programa>> = _programasAsociados.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (episodeId != -1) {
            loadEpisodeDetails()
        } else {
            _error.value = "ID de episodio no válido."
            Timber.e("EpisodeDetailViewModel inicializado con episodeId inválido.")
        }
    }

    fun loadEpisodeDetails() {
        if (episodeId == -1) {
            _error.value = "ID de episodio no válido para cargar detalles."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fetchedEpisodio = wordpressService.getEpisodio(episodeId)
                _episodio.value = fetchedEpisodio
                Timber.d("EpisodeDetailViewModel: Episodio cargado: ${fetchedEpisodio?.title}")

                // Cargar nombres de programas asociados si el episodio tiene IDs de programa
                fetchedEpisodio?.programaIds?.let { ids ->
                    if (ids.isNotEmpty()) {
                        val embeddedProgramas = fetchedEpisodio.embedded?.terms
                            ?.flatten()
                            ?.filter { programa -> ids.contains(programa.id) }
                        if (!embeddedProgramas.isNullOrEmpty()) {
                            _programasAsociados.value = embeddedProgramas
                            Timber.d("EpisodeDetailViewModel: Programas asociados obtenidos de _embedded: ${embeddedProgramas.joinToString { it.name }}")
                        } else {
                            Timber.w("EpisodeDetailViewModel: No se encontraron programas embebidos completos, se podría implementar un fallback si es necesario.")
                        }
                    }
                }

                if (fetchedEpisodio == null) {
                    _error.value = "No se pudo encontrar el episodio."
                }

            } catch (e: NoInternetException) {
                Timber.e(e, "EpisodeDetailViewModel: Error de red")
                _error.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                Timber.e(e, "EpisodeDetailViewModel: Error de servidor")
                _error.value = e.userFriendlyMessage
            } catch (e: Exception) {
                Timber.e(e, "EpisodeDetailViewModel: Error inesperado")
                _error.value = "Ocurrió un error al cargar los detalles del episodio."
            } finally {
                _isLoading.value = false
            }
        }
    }
}