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
        viewModelScope.launch {
            _isLoadingPrograma.value = true
            _isLoadingEpisodios.value = true
            _error.value = null
            try {
                // Primero, intenta obtener el programa si no lo tenemos.
                // Si la API de WordPress no tiene un endpoint para obtener un término de taxonomía por ID
                // directamente con todos sus campos personalizados (como imageUrl),
                // podríamos necesitar obtenerlo de una lista general o construirlo parcialmente.
                // WordpressService.getPrograma(programaId) no existe, así que lo construiremos parcialmente.
                // O mejor, WordpressService.getProgramas() podría usarse para encontrarlo si ya fue cargado.
                // Por ahora, asumimos que el nombre ya nos llegó y construimos un Programa básico.
                // Si tu WordpressService tuviera un `getProgramaById`, lo usarías aquí.
                // Por ahora, como el nombre viene por argumento, lo usamos.
                // La imagen del programa es un desafío si no hay un endpoint directo.
                _programa.value = Programa(id = programaId, name = programaNombre, slug = "", description = "Cargando descripción...") // Placeholder

                // Luego carga los episodios
                val fetchedEpisodios = wordpressService.getEpisodiosPorPrograma(programaId)
                _episodios.value = fetchedEpisodios
                Timber.d("Cargados ${fetchedEpisodios.size} episodios para el programa ID $programaId.")

                // Si tu API de WordPress devuelve la descripción del programa junto con los episodios
                // o si tienes un endpoint para obtener los detalles del programa, actualiza _programa.value aquí.
                // Ejemplo: val detallesPrograma = wordpressService.getDetallesPrograma(programaId)
                // _programa.value = detallesPrograma

            } catch (e: Exception) {
                Timber.e(e, "Error cargando programa (ID: $programaId) y sus episodios.")
                _error.value = "No se pudo cargar la información del programa."
            } finally {
                _isLoadingPrograma.value = false // Asumimos que la info básica del programa se cargó o se construyó
                _isLoadingEpisodios.value = false
            }
        }
    }
}