package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.model.Episodio // Modelo del módulo shared
import com.example.paradigmaapp.repository.WordpressService // Servicio del módulo shared
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel para la pantalla de búsqueda.
 * Gestiona el estado de la búsqueda, los resultados y la carga.
 * Ahora filtra los resultados del servidor para que coincidan con la frase exacta en el título.
 *
 * @property wordpressService El servicio para obtener datos de WordPress.
 * @author Mario Alguacil Juárez
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val wordpressService: WordpressService) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Episodio>>(emptyList())
    val searchResults: StateFlow<List<Episodio>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _searchText
                .debounce(400) // Espera 400ms después de la última entrada antes de buscar
                .distinctUntilChanged() // Solo busca si el texto ha cambiado
                .collectLatest { query ->
                    if (query.length > 2) { // Solo busca si la query tiene al menos 3 caracteres
                        performSearchAndFilter(query)
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        _searchError.value = null // Limpiar error si la query es muy corta
                        searchJob?.cancel() // Cancelar cualquier búsqueda en curso
                    }
                }
        }
    }

    /**
     * Actualiza el texto de búsqueda.
     * @param query El nuevo texto de búsqueda.
     */
    fun onSearchTextChanged(query: String) {
        _searchText.value = query
    }

    /**
     * Realiza la búsqueda de episodios en el servidor y luego filtra los resultados
     * para encontrar coincidencias de frase exacta en el título.
     * @param query El término de búsqueda (la frase).
     */
    private fun performSearchAndFilter(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResults.value = emptyList() // Limpiar resultados anteriores inmediatamente
            try {
                Timber.d("SearchViewModel: Realizando búsqueda en servidor para: $query")
                // Pequeño delay para que el usuario vea el indicador de carga si la respuesta es muy rápida
                delay(200) // Opcional, para mejorar UX visual

                // Paso 1: Obtener resultados del servidor (búsqueda general de WordPress)
                val serverResults = wordpressService.buscarEpisodios(query) //
                Timber.d("SearchViewModel: Resultados del servidor: ${serverResults.size}")

                // Paso 2: Filtrar en el cliente para frase exacta en el título (ignorando mayúsculas/minúsculas)
                val filteredResults = serverResults.filter { episodio ->
                    // episodio.title ya es el título renderizado y decodificado de entidades HTML
                    episodio.title.contains(query, ignoreCase = true)
                }
                Timber.d("SearchViewModel: Resultados filtrados por título (frase exacta): ${filteredResults.size}")

                _searchResults.value = filteredResults

                if (filteredResults.isEmpty()) {
                    _searchError.value = "No se encontraron episodios con el título \"$query\"."
                }

            } catch (e: Exception) {
                Timber.e(e, "SearchViewModel: Error durante la búsqueda y filtrado para query '$query'")
                _searchError.value = "Error al realizar la búsqueda. Inténtalo de nuevo."
                _searchResults.value = emptyList() // Limpiar en caso de error
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Limpia el texto de búsqueda y los resultados.
     */
    fun clearSearch() {
        _searchText.value = ""
        // El colector de _searchText se encargará de limpiar los resultados y el estado.
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel() // Cancela cualquier trabajo de búsqueda pendiente al limpiar el ViewModel
    }
}