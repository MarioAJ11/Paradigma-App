package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository // Usar la interfaz
import com.example.paradigmaapp.repository.contracts.ProgramaRepository // Usar la interfaz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para mi pantalla que muestra los detalles de un programa específico
 * y la lista de sus episodios asociados. Utilizo [SavedStateHandle] para
 * recibir el ID y nombre del programa pasados a través de la navegación.
 *
 * @author Mario Alguacil Juárez
 */
class ProgramaViewModel(
    // Ahora dependo de las abstracciones del repositorio.
    private val programaRepository: ProgramaRepository,
    private val episodioRepository: EpisodioRepository,
    private val savedStateHandle: SavedStateHandle // Para acceder a los argumentos de navegación.
) : ViewModel() {

    // Obtengo el ID y el nombre del programa desde los argumentos pasados por navegación.
    // Si no se encuentran, uso valores por defecto o de error.
    val programaId: Int = savedStateHandle.get<Int>("programaId") ?: -1
    val programaNombre: String = savedStateHandle.get<String>("programaNombre") ?: "Programa Desconocido"

    // Estado para el objeto Programa actual (con sus detalles).
    private val _programa = MutableStateFlow<Programa?>(null)
    val programa: StateFlow<Programa?> = _programa.asStateFlow()

    // Estado para la lista de episodios del programa actual.
    private val _episodios = MutableStateFlow<List<Episodio>>(emptyList())
    val episodios: StateFlow<List<Episodio>> = _episodios.asStateFlow()

    // Indica si se están cargando los detalles del programa.
    private val _isLoadingPrograma = MutableStateFlow(false)
    val isLoadingPrograma: StateFlow<Boolean> = _isLoadingPrograma.asStateFlow()

    // Indica si se está cargando la lista de episodios del programa.
    private val _isLoadingEpisodios = MutableStateFlow(false)
    val isLoadingEpisodios: StateFlow<Boolean> = _isLoadingEpisodios.asStateFlow()

    // Almacena mensajes de error que pueden ocurrir durante la carga de datos.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Si el programaId es válido, inicio la carga de los detalles del programa y sus episodios.
        if (programaId != -1) {
            loadProgramaConEpisodios()
        } else {
            // Si el ID no es válido, establezco un error inmediatamente.
            _error.value = "ID de programa no válido."
        }
    }

    /**
     * Carga los detalles del programa actual (usando [programaId]) y luego
     * la lista de sus episodios asociados. Gestiona los estados de carga y error.
     */
    fun loadProgramaConEpisodios() {
        if (programaId == -1) {
            _error.value = "ID de programa no válido para cargar episodios."
            // ...
            return
        }

        viewModelScope.launch {
            _isLoadingPrograma.value = true
            _isLoadingEpisodios.value = true
            _error.value = null

            try {
                val todosLosProgramas = programaRepository.getProgramas()
                val programaActualCompleto = todosLosProgramas.find { it.id == programaId }
                _isLoadingPrograma.value = false // Detalles del programa cargados (o fallback)

                if (programaActualCompleto != null) {
                    _programa.value = programaActualCompleto
                    val episodeCount = programaActualCompleto.count ?: 30 // Usar count o un default

                    // WordPress tiene un límite máximo para per_page (usualmente 100).
                    // Si episodeCount es mayor, necesitaríamos paginar.
                    // Por simplicidad aquí, si count es > 100, pedimos 100.
                    // Una solución completa requeriría múltiples llamadas si count > 100.
                    val perPageEffective = if (episodeCount > 0 && episodeCount <= 100) episodeCount else 30
                    // Si count es 0, podríamos pedir 1 para obtener una respuesta vacía
                    // o directamente no hacer la llamada si sabemos que no hay episodios.
                    // Si es > 100, usar 30 (o 100 y luego implementar paginación completa)

                    if (episodeCount == 0) {
                        _episodios.value = emptyList()
                    } else {
                        val fetchedEpisodios = episodioRepository.getEpisodiosPorPrograma(
                            programaId = programaId,
                            page = 1, // Asumimos que queremos la primera página (todos si perPageEffective cubre el count)
                            perPage = perPageEffective
                        )
                        _episodios.value = fetchedEpisodios
                    }

                } else {
                    _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Descripción no disponible")
                    // Si no podemos obtener el programa, ¿qué hacemos con los episodios?
                    // Podríamos intentar cargarlos con un perPage por defecto o mostrar error.
                    _error.value = "No se pudo encontrar la información completa del programa."
                    _episodios.value = emptyList()
                }

            } catch (e: NoInternetException) {
                _error.value = e.message ?: "Sin conexión a internet."
                _episodios.value = emptyList()
            } catch (e: ServerErrorException) {
                _error.value = e.userFriendlyMessage
                _episodios.value = emptyList()
            } catch (e: Exception) {
                _error.value = "No se pudo cargar la información del programa o episodios: ${e.localizedMessage}"
                _episodios.value = emptyList()
            } finally {
                _isLoadingPrograma.value = false // Asegurarse de que ambos se ponen a false
                _isLoadingEpisodios.value = false
            }
        }
    }
}