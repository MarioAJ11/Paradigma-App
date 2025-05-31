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
            // Doble verificación, aunque init ya lo hace, por si se llama externamente.
            _error.value = "ID de programa no válido para cargar episodios."
            _isLoadingEpisodios.value = false
            _isLoadingPrograma.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingPrograma.value = true
            _isLoadingEpisodios.value = true
            _error.value = null // Limpio errores anteriores al iniciar una nueva carga.

            try {
                // Obtengo todos los programas para encontrar el actual con todos sus detalles
                // (esto podría optimizarse si tuviera un endpoint para un solo programa por ID con toda su info).
                val todosLosProgramas = programaRepository.getProgramas()
                val programaActualCompleto = todosLosProgramas.find { it.id == programaId }

                if (programaActualCompleto != null) {
                    _programa.value = programaActualCompleto // Programa encontrado y asignado.
                } else {
                    // Si no se encuentra el programa (raro si el ID es válido),
                    // uso un objeto Programa de fallback con el nombre pasado y una descripción genérica.
                    // Considero si esto debería ser un error más fuerte.
                    _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Descripción no disponible")
                    // _error.value = "No se pudo encontrar la información completa del programa." // Opcional
                }
                _isLoadingPrograma.value = false // Termina la carga de detalles del programa.

                // Ahora cargo los episodios para este programa.
                val fetchedEpisodios = episodioRepository.getEpisodiosPorPrograma(programaId = programaId, perPage = 30)
                _episodios.value = fetchedEpisodios

            } catch (e: NoInternetException) {
                _error.value = e.message ?: "Sin conexión a internet."
                _episodios.value = emptyList() // Limpio episodios en caso de error.
            } catch (e: ServerErrorException) {
                _error.value = e.userFriendlyMessage
                _episodios.value = emptyList()
            } catch (e: Exception) {
                _error.value = "No se pudo cargar la información del programa o episodios."
                _episodios.value = emptyList()
            } finally {
                // Aseguro que los indicadores de carga se desactiven.
                _isLoadingPrograma.value = false
                _isLoadingEpisodios.value = false
            }
        }
    }
}