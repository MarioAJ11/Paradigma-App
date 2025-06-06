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
import com.example.paradigmaapp.repository.contracts.ProgramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla que muestra los detalles de un [Programa] específico
 * y la lista de sus [Episodio]s asociados.
 * Utiliza [SavedStateHandle] para recibir el ID y nombre del programa pasados
 * a través de la navegación.
 *
 * @property programaRepository Repositorio para obtener datos de programas.
 * @property episodioRepository Repositorio para obtener datos de episodios.
 * @property savedStateHandle Manejador para acceder a los argumentos de navegación.
 *
 * @author Mario Alguacil Juárez
 */
class ProgramaViewModel(
    private val programaRepository: ProgramaRepository, //
    private val episodioRepository: EpisodioRepository, //
    private val savedStateHandle: SavedStateHandle //
) : ViewModel() {

    /** El ID del programa actual, obtenido de los argumentos de navegación. */
    val programaId: Int = savedStateHandle.get<Int>("programaId") ?: -1 //
    /** El nombre del programa actual, obtenido de los argumentos de navegación. */
    val programaNombre: String = savedStateHandle.get<String>("programaNombre") ?: "Programa Desconocido" //

    private val _programa = MutableStateFlow<Programa?>(null)
    val programa: StateFlow<Programa?> = _programa.asStateFlow()

    private val _episodios = MutableStateFlow<List<Episodio>>(emptyList())
    val episodios: StateFlow<List<Episodio>> = _episodios.asStateFlow()

    private val _isLoadingPrograma = MutableStateFlow(false)
    val isLoadingPrograma: StateFlow<Boolean> = _isLoadingPrograma.asStateFlow()

    private val _isLoadingEpisodios = MutableStateFlow(false)
    val isLoadingEpisodios: StateFlow<Boolean> = _isLoadingEpisodios.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Límite máximo de episodios a solicitar por página si el conteo del programa es muy alto o desconocido.
    // La API de WordPress generalmente tiene un máximo de 100 para 'per_page'.
    private val MAX_EPISODES_PER_PAGE_API_LIMIT = 100
    private val DEFAULT_EPISODES_PER_PAGE = 30


    init {
        if (programaId != -1) {
            loadProgramaConEpisodios()
        } else {
            _error.value = "ID de programa no válido."
        }
    }

    /**
     * Carga los detalles del programa actual (usando [programaId]) y luego
     * la lista de sus episodios asociados. Gestiona los estados de carga y error.
     * Intenta obtener todos los episodios de un programa si el conteo es conocido y
     * dentro de los límites de la API, de lo contrario, pide una cantidad por defecto.
     */
    fun loadProgramaConEpisodios() {
        if (programaId == -1) {
            _error.value = "ID de programa no válido para cargar episodios."
            return
        }

        viewModelScope.launch {
            _isLoadingPrograma.value = true
            _isLoadingEpisodios.value = true // Indicar carga para ambos al inicio
            _error.value = null // Limpiar errores anteriores

            try {
                // Paso 1: Obtener los detalles del programa.
                // Esto es necesario para obtener el campo 'count' de episodios.
                val todosLosProgramas = programaRepository.getProgramas()
                val programaActualCompleto = todosLosProgramas.find { it.id == programaId }
                _isLoadingPrograma.value = false // Termina la carga de detalles del programa.

                if (programaActualCompleto != null) {
                    _programa.value = programaActualCompleto

                    // Paso 2: Cargar episodios usando el 'count' del programa si está disponible.
                    val episodeCount = programaActualCompleto.count
                    if (episodeCount == 0) {
                        _episodios.value = emptyList() // No hay episodios, no hacer llamada.
                    } else {
                        val perPageEffective = when {
                            episodeCount == null -> DEFAULT_EPISODES_PER_PAGE // Count desconocido, usar default.
                            episodeCount > 0 && episodeCount <= MAX_EPISODES_PER_PAGE_API_LIMIT -> episodeCount // Count conocido y dentro del límite.
                            else -> MAX_EPISODES_PER_PAGE_API_LIMIT // Count excede el límite, pedir el máximo posible (y requeriría paginación para más).
                        }
                        val fetchedEpisodios = episodioRepository.getEpisodiosPorPrograma(
                            programaId = programaId,
                            page = 1, // Asumimos que queremos la primera página (todos si perPageEffective cubre el count)
                            perPage = perPageEffective
                        )
                        _episodios.value = fetchedEpisodios
                    }
                } else {
                    // El programa no se encontró, establecer error y usar datos de fallback.
                    _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Descripción no disponible")
                    _error.value = "No se pudo encontrar la información completa del programa."
                    _episodios.value = emptyList() // No se pueden cargar episodios si el programa no se encuentra.
                }

            } catch (e: NoInternetException) {
                _error.value = e.message ?: "Sin conexión a internet."
                _episodios.value = emptyList() // Limpiar episodios en caso de error de red
            } catch (e: ServerErrorException) {
                _error.value = e.userFriendlyMessage
                _episodios.value = emptyList()
            } catch (e: ApiException) {
                _error.value = e.message ?: "Error de API al cargar datos del programa."
                _episodios.value = emptyList()
            } catch (e: Exception) {
                _error.value = "Ocurrió un error desconocido."
                _episodios.value = emptyList()
                // Considerar registrar `e` en un sistema de monitoreo.
            } finally {
                // Asegurar que ambos indicadores de carga se desactiven.
                _isLoadingPrograma.value = false // Ya se puso a false antes, pero por si acaso.
                _isLoadingEpisodios.value = false
            }
        }
    }
}