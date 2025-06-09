package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.ParadigmaRepository
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de búsqueda de episodios.
 * Implementa una estrategia de búsqueda híbrida para una experiencia de usuario óptima:
 * 1.  **Búsqueda local instantánea**: Ofrece resultados inmediatos desde la caché.
 * 2.  **Búsqueda en red**: Complementa los resultados con una búsqueda completa en el servidor.
 *
 * @property episodioRepository Repositorio para buscar episodios. Debe ser una instancia de [ParadigmaRepository].
 * @author Mario Alguacil Juárez
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val episodioRepository: EpisodioRepository
) : ViewModel() {

    // Flujo para el texto de búsqueda introducido por el usuario.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // Flujo para la lista de episodios que coinciden con la búsqueda.
    private val _searchResults = MutableStateFlow<List<Episodio>>(emptyList())
    val searchResults: StateFlow<List<Episodio>> = _searchResults.asStateFlow()

    // Flujo para indicar si una búsqueda está en progreso.
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Flujo para cualquier mensaje de error.
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchJob: Job? = null

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        internal const val MIN_QUERY_LENGTH = 3
    }

    init {
        // Observa los cambios en el texto para lanzar la búsqueda automáticamente.
        observeSearchTextChanges()
    }

    /** Observa los cambios en el texto de búsqueda con un debounce. */
    private fun observeSearchTextChanges() {
        viewModelScope.launch {
            _searchText
                .debounce(SEARCH_DEBOUNCE_MS)
                .filter { query -> query.length >= MIN_QUERY_LENGTH || query.isEmpty() }
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length >= MIN_QUERY_LENGTH) {
                        performSearch(query)
                    } else {
                        // Limpia los resultados si la consulta es muy corta o está vacía.
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        _searchError.value = null
                        searchJob?.cancel()
                    }
                }
        }
    }

    /** Actualiza el texto de búsqueda desde la UI. */
    fun onSearchTextChanged(query: String) {
        _searchText.value = query
    }

    /**
     * Realiza la búsqueda híbrida. Primero consulta la caché local para resultados
     * instantáneos y luego busca en la red para obtener una lista completa.
     *
     * @param query El término de búsqueda.
     */
    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null

            // 1. Búsqueda local para resultados instantáneos (sin cambios).
            val localResults = (episodioRepository as? ParadigmaRepository)
                ?.buscarEpisodiosEnCache(query) ?: emptyList()
            _searchResults.value = localResults

            // 2. Búsqueda en red para resultados completos y manejo de errores mejorado.
            try {
                val networkResults = episodioRepository.buscarEpisodios(query)

                // Combina y elimina duplicados
                val combinedResults = (localResults + networkResults).distinctBy { it.id }

                if (combinedResults.isNotEmpty()) {
                    _searchResults.value = combinedResults
                    // Limpiamos cualquier error previo si ahora encontramos resultados.
                    _searchError.value = null
                } else {
                    // Si, tras buscar en la red, la lista combinada sigue vacía,
                    // es una búsqueda sin resultados, NO un error.
                    _searchResults.value = emptyList()
                    _searchError.value = "Búsqueda no encontrada para \"$query\"."
                }

            } catch (e: NoInternetException) {
                val errorMessage = if (localResults.isEmpty()) {
                    "No se pudo conectar. Revisa tu conexión a internet."
                } else {
                    "Mostrando resultados locales. Se requiere conexión para una búsqueda completa."
                }
                _searchError.value = errorMessage
            } catch (e: Exception) {
                _searchError.value = "Ocurrió un error al realizar la búsqueda."
                // Si hay un error y no teníamos resultados locales, limpiamos la lista.
                if (localResults.isEmpty()) _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /** Limpia el texto de búsqueda y los resultados. */
    fun clearSearch() {
        _searchText.value = ""
    }

    /** Permite reintentar la última búsqueda realizada. */
    fun retrySearch() {
        val currentQuery = _searchText.value
        if (currentQuery.length >= MIN_QUERY_LENGTH) {
            performSearch(currentQuery)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}