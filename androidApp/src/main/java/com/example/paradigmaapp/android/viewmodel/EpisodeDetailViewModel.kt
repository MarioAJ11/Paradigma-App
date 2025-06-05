package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalle de un [Episodio].
 * Se encarga de cargar la información completa del episodio especificado por su ID,
 * el cual se recibe a través de [SavedStateHandle] desde los argumentos de navegación.
 * También extrae los programas asociados al episodio a partir de los datos embebidos.
 *
 * @property episodioRepository Repositorio para obtener los datos del episodio.
 * @property savedStateHandle Manejador del estado guardado, utilizado para acceder a los
 * argumentos de navegación (como `episodeId`).
 *
 * @author Mario Alguacil Juárez
 */
class EpisodeDetailViewModel(
    private val episodioRepository: EpisodioRepository, //
    private val savedStateHandle: SavedStateHandle //
) : ViewModel() {

    /**
     * El ID del episodio cuyos detalles se van a mostrar.
     * Se obtiene de los argumentos de navegación a través de [SavedStateHandle].
     * Si no se encuentra, se establece a -1, indicando un estado inválido.
     */
    val episodeId: Int = savedStateHandle.get<Int>("episodeId") ?: -1 //

    // StateFlow para el objeto Episodio actual, con todos sus detalles.
    private val _episodio = MutableStateFlow<Episodio?>(null)
    val episodio: StateFlow<Episodio?> = _episodio.asStateFlow()

    // StateFlow para la lista de programas ([Programa]) asociados a este episodio.
    // Se utiliza para mostrar información como los creadores o el programa al que pertenece.
    private val _programasAsociados = MutableStateFlow<List<Programa>>(emptyList())
    val programasAsociados: StateFlow<List<Programa>> = _programasAsociados.asStateFlow()

    // StateFlow para indicar si los detalles del episodio se están cargando actualmente.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // StateFlow para almacenar cualquier mensaje de error que ocurra durante la carga de datos.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (episodeId != -1) {
            loadEpisodeDetails()
        } else {
            _error.value = "ID de episodio no válido."
        }
    }

    /**
     * Carga los detalles completos del episodio utilizando el [episodeId] actual.
     * Obtiene el episodio del [episodioRepository] y extrae los programas asociados
     * de los datos embebidos (`_embedded.wp:term`) del episodio.
     * Actualiza los StateFlows correspondientes ([_episodio], [_programasAsociados], [_isLoading], [_error]).
     */
    fun loadEpisodeDetails() {
        if (episodeId == -1) {
            _error.value = "ID de episodio no válido. No se pueden cargar los detalles."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Limpiar errores previos al iniciar la carga
            try {
                val fetchedEpisodio = episodioRepository.getEpisodio(episodeId) //
                _episodio.value = fetchedEpisodio

                if (fetchedEpisodio != null) {
                    // Extraer programas asociados de los datos embebidos (_embedded.wp:term)
                    fetchedEpisodio.embedded?.terms?.let { terminosAnidados ->
                        val programasEncontrados = terminosAnidados.flatten()
                            .distinctBy { programa -> programa.id } //

                        // Filtrar para asegurar que solo se incluyen los programas cuyos IDs están
                        // realmente listados en `fetchedEpisodio.programaIds`.
                        // Esto proporciona una capa adicional de consistencia.
                        _programasAsociados.value = fetchedEpisodio.programaIds?.let { idsDelEpisodio ->
                            programasEncontrados.filter { programa -> idsDelEpisodio.contains(programa.id) }
                        } ?: programasEncontrados // Fallback a todos los encontrados si programaIds es null
                    } ?: run {
                        _programasAsociados.value = emptyList() // No hay términos o 'embedded' es null
                    }
                } else {
                    // Si fetchedEpisodio es null y no hubo una excepción previa (ej. 404 manejado por repo),
                    // se establece un error genérico de "no encontrado".
                    if (_error.value == null) { // Solo si no hay un error más específico ya
                        _error.value = "No se pudo encontrar el episodio."
                    }
                }

            } catch (e: NoInternetException) {
                _error.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                _error.value = e.userFriendlyMessage
            } catch (e: ApiException) {
                _error.value = e.message ?: "Error de API al cargar detalles del episodio."
            } catch (e: Exception) {
                _error.value = "Ocurrió un error desconocido al cargar los detalles del episodio."
                // Considerar registrar `e` en un sistema de monitoreo para producción.
            } finally {
                _isLoading.value = false
            }
        }
    }
}