package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.paradigmaapp.android.data.EpisodioPagingSource
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import com.example.paradigmaapp.repository.contracts.ProgramaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalles de un Programa.
 * Sus responsabilidades son:
 * 1. Cargar los detalles del programa específico (imagen, descripción) para la cabecera.
 * 2. Proveer un flujo de datos paginados (`Flow<PagingData<Episodio>>`) para la lista de episodios,
 * utilizando la librería Jetpack Paging 3.
 *
 * @param programaRepository Repositorio para obtener datos del programa.
 * @param episodioRepository Repositorio para obtener los episodios.
 * @param savedStateHandle Manejador para acceder a los argumentos de navegación.
 *
 * @author Mario Alguacil Juárez
 */
class ProgramaViewModel(
    private val programaRepository: ProgramaRepository,
    private val episodioRepository: EpisodioRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ARGUMENTOS DE NAVEGACIÓN ---
    /** El ID del programa actual, obtenido de los argumentos de navegación. */
    val programaId: Int = savedStateHandle.get<Int>("programaId") ?: -1
    /** El nombre del programa actual, usado como fallback si los detalles no cargan. */
    val programaNombre: String = savedStateHandle.get<String>("programaNombre") ?: "Programa"

    // --- ESTADO PARA LA CABECERA ---
    /** StateFlow privado y mutable para los detalles del programa. */
    private val _programa = MutableStateFlow<Programa?>(null)
    /** StateFlow público e inmutable que la UI observará para la cabecera. */
    val programa: StateFlow<Programa?> = _programa.asStateFlow()

    // --- FLUJO DE DATOS PAGINADOS PARA LA LISTA ---
    /**
     * Flujo de datos paginados que la UI conectará a la LazyColumn.
     * `Pager` es el componente principal de Paging 3 que construye el flujo.
     */
    val episodiosPaginados: Flow<PagingData<Episodio>> = Pager(
        // Configuración de cómo se deben cargar las páginas.
        config = PagingConfig(
            pageSize = 20, // El número de elementos a cargar en cada página.
            enablePlaceholders = false
        ),
        // Le decimos a Paging que use nuestro EpisodioPagingSource para obtener los datos.
        pagingSourceFactory = { EpisodioPagingSource(episodioRepository, programaId) }
    )
        .flow
        // Cachea los resultados en el ViewModelScope para que los datos sobrevivan a cambios
        // de configuración como la rotación de la pantalla, evitando recargas innecesarias.
        .cachedIn(viewModelScope)

    /**
     * El bloque init se ejecuta al crear el ViewModel y lanza la carga
     * de los detalles del programa para la cabecera.
     */
    init {
        loadProgramaDetails()
    }

    /**
     * Carga los detalles de un único programa de forma eficiente usando su ID.
     */
    private fun loadProgramaDetails() {
        // Lanzamos la corutina en el scope del ViewModel.
        viewModelScope.launch {
            // Llamamos a la nueva función eficiente del repositorio.
            val programaDetails = programaRepository.getPrograma(programaId)
            if (programaDetails != null) {
                // Si la llamada es exitosa, actualizamos el estado.
                _programa.value = programaDetails
            } else {
                // Si el programa no se encuentra (ej. 404), creamos un objeto básico
                // para al menos mostrar el título correcto en la cabecera.
                _programa.value = Programa(id = programaId, name = programaNombre, slug = "")
            }
        }
    }
}