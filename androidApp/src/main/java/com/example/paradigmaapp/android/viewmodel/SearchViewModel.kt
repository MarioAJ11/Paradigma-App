package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel para la pantalla de búsqueda.
 * Gestiona el estado de la búsqueda, los resultados y la carga.
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
                .collectLatest { query -> // collectLatest cancela la búsqueda anterior si llega una nueva query
                    if (query.length > 2) { // Solo busca si la query tiene al menos 3 caracteres
                        performSearch(query)
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
     * Realiza la búsqueda de episodios.
     * @param query El término de búsqueda.
     */
    private fun performSearch(query: String) {
        // collectLatest ya cancela el bloque anterior, pero por si acaso se llama externamente.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResults.value = emptyList() // Limpiar resultados anteriores inmediatamente
            try {
                Timber.d("Realizando búsqueda para: $query")
                // Pequeño delay para que el usuario vea el indicador de carga si la respuesta es muy rápida
                delay(200)
                val results = wordpressService.buscarEpisodios(query)
                _searchResults.value = results
                Timber.d("Resultados de búsqueda: ${results.size}")
                if (results.isEmpty()) {
                    _searchError.value = "No se encontraron resultados para \"$query\"."
                }
            } catch (e: Exception) {
                Timber.e(e, "Error durante la búsqueda para query '$query'")
                _searchError.value = "Error al realizar la búsqueda. Inténtalo de nuevo."
                _searchResults.value = emptyList()
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
        searchJob?.cancel()
    }
}