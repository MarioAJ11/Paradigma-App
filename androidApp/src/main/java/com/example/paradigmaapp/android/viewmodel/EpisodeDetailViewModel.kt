package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
// Si necesitaras cargar info de programas de forma independiente:
// import com.example.paradigmaapp.repository.contracts.ProgramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para mi pantalla de detalle de un episodio.
 * Se encarga de cargar la información completa del episodio especificado por su ID,
 * el cual recibo a través de [SavedStateHandle]. También puede cargar información
 * relacionada, como los programas a los que pertenece el episodio.
 *
 * @author Mario Alguacil Juárez
 */
class EpisodeDetailViewModel(
    // Ahora dependo de la abstracción del repositorio de episodios.
    private val episodioRepository: EpisodioRepository,
    // Si necesitara cargar info de programas de forma independiente:
    // private val programaRepository: ProgramaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Obtengo el ID del episodio desde los argumentos de navegación.
    val episodeId: Int = savedStateHandle.get<Int>("episodeId") ?: -1

    // Estado para el objeto Episodio actual con sus detalles.
    private val _episodio = MutableStateFlow<Episodio?>(null)
    val episodio: StateFlow<Episodio?> = _episodio.asStateFlow()

    // Estado para la lista de programas asociados a este episodio (usado para mostrar "Creadores").
    private val _programasAsociados = MutableStateFlow<List<Programa>>(emptyList())
    val programasAsociados: StateFlow<List<Programa>> = _programasAsociados.asStateFlow()

    // Indica si se están cargando los detalles del episodio.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Almacena mensajes de error que pueden ocurrir durante la carga.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Si el episodeId es válido, inicio la carga de los detalles.
        if (episodeId != -1) {
            loadEpisodeDetails()
        } else {
            _error.value = "ID de episodio no válido."
        }
    }

    /**
     * Carga los detalles del episodio actual (usando [episodeId]) desde el repositorio.
     * También intenta obtener los programas asociados a partir de los datos embebidos del episodio.
     */
    fun loadEpisodeDetails() {
        if (episodeId == -1) {
            _error.value = "ID de episodio no válido para cargar detalles."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Limpio errores anteriores.
            try {
                // Cargo el episodio usando la interfaz del repositorio.
                val fetchedEpisodio = episodioRepository.getEpisodio(episodeId)
                _episodio.value = fetchedEpisodio

                // Intento obtener los nombres de los programas asociados desde los datos embebidos del episodio.
                // El modelo Episodio (con _embed=wp:term) ya debería incluir esta información.
                // Las propiedades aquí deben coincidir con tu clase Episodio.kt (en inglés).
                fetchedEpisodio?.embedded?.terms?.let { terminosAnidados -> // Uso 'embedded' y 'terms'
                    // 'terms' es List<List<Programa>>, lo aplano para obtener una sola lista de Programas.
                    val programasEncontrados = terminosAnidados.flatten().distinctBy { programa -> programa.id } // 'it' ahora es 'programa' para claridad

                    // Filtro para asegurar que solo incluyo los programas cuyos IDs están en 'fetchedEpisodio.programaIds',
                    // aunque 'terms' ya debería estar filtrado por el backend para este post.
                    // 'programaIds' es la propiedad correcta de la clase Episodio.
                    _programasAsociados.value = fetchedEpisodio.programaIds?.let { idsPresentesEnEpisodio ->
                        programasEncontrados.filter { programa -> idsPresentesEnEpisodio.contains(programa.id) }
                    } ?: programasEncontrados // Si programaIds es null, tomo todos los programas de los términos (menos probable pero posible)
                }


                if (fetchedEpisodio == null && _error.value == null) {
                    // Si el episodio es null y no hubo una excepción (ej. 404 manejado por el repo),
                    // establezco un error genérico de "no encontrado".
                    _error.value = "No se pudo encontrar el episodio."
                }

            } catch (e: NoInternetException) {
                _error.value = e.message ?: "Sin conexión a internet."
            } catch (e: ServerErrorException) {
                _error.value = e.userFriendlyMessage
            } catch (e: Exception) {
                _error.value = "Ocurrió un error al cargar los detalles del episodio."
            } finally {
                _isLoading.value = false
            }
        }
    }
}