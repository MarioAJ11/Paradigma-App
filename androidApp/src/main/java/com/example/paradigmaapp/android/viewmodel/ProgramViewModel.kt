package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ProgramaViewModel(
    private val wordpressService: WordpressService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

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

    private val _error = MutableStateFlow<String?>(null) // Mensaje de error genérico para la UI
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (programaId != -1) {
            loadProgramaConEpisodios()
        } else {
            _error.value = "ID de programa no válido."
            Timber.e("ProgramaViewModel inicializado con programaId inválido.")
        }
    }

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
            _error.value = null // Limpiar error anterior
            Timber.d("ProgramaViewModel: Iniciando carga de programa y episodios para programaId: $programaId, Nombre (pasado): $programaNombre")

            try {
                val todosLosProgramas = wordpressService.getProgramas()
                val programaActualCompleto = todosLosProgramas.find { it.id == programaId }

                if (programaActualCompleto != null) {
                    _programa.value = programaActualCompleto
                    Timber.d("ProgramaViewModel: Detalles completos del programa (ID: $programaId) cargados.")
                } else {
                    _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Descripción no disponible")
                    Timber.w("ProgramaViewModel: No se encontró el programa completo con ID $programaId.")
                    // Considera establecer un error aquí si el programa DEBERÍA existir.
                    // _error.value = "No se pudo encontrar la información del programa."
                }
                _isLoadingPrograma.value = false // Programa cargado o fallback asignado

                Timber.d("ProgramaViewModel: Llamando a wordpressService.getEpisodiosPorPrograma con programaId: $programaId")
                val fetchedEpisodios = wordpressService.getEpisodiosPorPrograma(programaId = programaId, perPage = 30)
                _episodios.value = fetchedEpisodios
                Timber.d("ProgramaViewModel: Recibidos ${fetchedEpisodios.size} episodios para el programa ID $programaId.")

            } catch (e: NoInternetException) {
                Timber.e(e, "ProgramaViewModel: Error de red cargando programa (ID: $programaId) y/o sus episodios.")
                _error.value = e.message ?: "Sin conexión a internet."
                _episodios.value = emptyList()
            } catch (e: ServerErrorException) {
                Timber.e(e, "ProgramaViewModel: Error de servidor cargando programa (ID: $programaId) y/o sus episodios.")
                _error.value = e.userFriendlyMessage
                _episodios.value = emptyList()
            } catch (e: Exception) {
                Timber.e(e, "ProgramaViewModel: Error inesperado cargando programa (ID: $programaId) y/o sus episodios.")
                _error.value = "No se pudo cargar la información del programa o episodios."
                _episodios.value = emptyList()
            } finally {
                _isLoadingPrograma.value = false // Asegurar que se resetea incluso si solo fallan los episodios
                _isLoadingEpisodios.value = false
            }
        }
    }
}