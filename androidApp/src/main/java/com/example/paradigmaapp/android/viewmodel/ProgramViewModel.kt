package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel para la pantalla que muestra los detalles de un programa y su lista de episodios.
 *
 * @property wordpressService Servicio para obtener datos de WordPress.
 * @property savedStateHandle Manejador para acceder a los argumentos de navegación (como programaId).
 * @author Mario Alguacil Juárez
 */
class ProgramaViewModel(
    private val wordpressService: WordpressService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Obtiene el ID y nombre del programa de los argumentos de navegación.
    val programaId: Int = savedStateHandle.get<Int>("programaId") ?: -1
    val programaNombre: String = savedStateHandle.get<String>("programaNombre") ?: "Programa"


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

    init {
        if (programaId != -1) {
            loadProgramaConEpisodios()
        } else {
            _error.value = "ID de programa no válido."
            Timber.e("ProgramaViewModel inicializado con programaId inválido.")
        }
    }

    /**
     * Carga los detalles del programa y luego sus episodios.
     */
    fun loadProgramaConEpisodios() {
        if (programaId == -1) {
            _error.value = "ID de programa no válido para cargar episodios."
            Timber.e("ProgramaViewModel: Intento de cargar episodios con programaId inválido (-1).")
            _isLoadingEpisodios.value = false
            _isLoadingPrograma.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingPrograma.value = true
            _isLoadingEpisodios.value = true
            _error.value = null
            Timber.d("ProgramaViewModel: Iniciando carga de programa y episodios para programaId: $programaId, Nombre (pasado): $programaNombre")

            try {

                val todosLosProgramas = wordpressService.getProgramas() // Vuelve a cargar todos los programas
                val programaActualCompleto = todosLosProgramas.find { it.id == programaId }

                if (programaActualCompleto != null) {
                    _programa.value = programaActualCompleto // Ahora tiene la descripción real
                    Timber.d("ProgramaViewModel: Detalles completos del programa (ID: $programaId) cargados, descripción: ${programaActualCompleto.description}")
                } else {
                    // Si no se encuentra (raro si el ID es válido), usa el nombre pasado y sin descripción.
                    _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Descripción no disponible")
                    Timber.w("ProgramaViewModel: No se encontró el programa completo con ID $programaId en la lista recargada.")
                }
                _isLoadingPrograma.value = false

                Timber.d("ProgramaViewModel: Llamando a wordpressService.getEpisodiosPorPrograma con programaId: $programaId y perPage: 30")
                val fetchedEpisodios = wordpressService.getEpisodiosPorPrograma(programaId = programaId, perPage = 30)
                _episodios.value = fetchedEpisodios
                Timber.d("ProgramaViewModel: Recibidos ${fetchedEpisodios.size} episodios para el programa ID $programaId.")

            } catch (e: Exception) {
                Timber.e(e, "ProgramaViewModel: Error cargando programa (ID: $programaId) y/o sus episodios.")
                _error.value = "No se pudo cargar la información del programa o episodios."
                _episodios.value = emptyList()
                _isLoadingPrograma.value = false // Asegurarse de resetear en error
            } finally {
                _isLoadingEpisodios.value = false
            }
        }
    }
}