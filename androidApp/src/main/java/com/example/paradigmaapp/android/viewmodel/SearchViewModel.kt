package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.ApiException
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
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
 * Gestiona el estado del texto de búsqueda, los resultados, el estado de carga
 * y los posibles errores. La búsqueda se activa cuando el usuario introduce al menos 3 caracteres,
 * con un debounce para evitar peticiones excesivas a la API.
 *
 * @property episodioRepository Repositorio para realizar la búsqueda de episodios.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(FlowPreview::class) // Necesario para el operador debounce
class SearchViewModel(
    private val episodioRepository: EpisodioRepository
) : ViewModel() {

    // StateFlow para el texto actual introducido por el usuario en la barra de búsqueda.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // StateFlow para la lista de episodios que coinciden con la búsqueda.
    private val _searchResults = MutableStateFlow<List<Episodio>>(emptyList())
    val searchResults: StateFlow<List<Episodio>> = _searchResults.asStateFlow()

    // StateFlow para indicar si una búsqueda está actualmente en progreso.
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // StateFlow para el mensaje de error si la búsqueda falla o no produce resultados.
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchJob: Job? = null

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        internal const val MIN_QUERY_LENGTH = 3
    }

    init {
        observeSearchTextChanges()
    }

    /** Observa los cambios en el texto de búsqueda para iniciar automáticamente la búsqueda. */
    private fun observeSearchTextChanges() {
        viewModelScope.launch {
            _searchText
                .debounce(SEARCH_DEBOUNCE_MS) // Espera a que el usuario deje de escribir
                .filter { query -> // Solo procesar si la query es suficientemente larga o está vacía (para limpiar)
                    query.length >= MIN_QUERY_LENGTH || query.isEmpty()
                }
                .distinctUntilChanged() // Evita búsquedas repetidas con el mismo texto
                .collectLatest { query -> // Cancela la búsqueda anterior si llega una nueva query
                    if (query.length >= MIN_QUERY_LENGTH) {
                        performSearch(query)
                    } else {
                        // Query corta o vacía, limpiar resultados y estado.
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        _searchError.value = null // Limpiar errores si la query se borra o es muy corta
                        searchJob?.cancel()
                    }
                }
        }
    }

    /**
     * Se invoca cuando el texto en la UI de búsqueda cambia. Actualiza el [searchText].
     * @param query El nuevo texto de búsqueda.
     */
    fun onSearchTextChanged(query: String) {
        _searchText.value = query
    }

    /** Realiza la búsqueda de episodios utilizando el [episodioRepository]. */
    private fun performSearch(query: String) {
        searchJob?.cancel() // Cancelar cualquier búsqueda anterior.
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null // Limpiar errores al iniciar una nueva búsqueda.
            _searchResults.value = emptyList() // Limpiar resultados anteriores.

            try {
                // La API de WordPress ya filtra por término de búsqueda en múltiples campos.
                // El filtrado adicional por título en el cliente se puede mantener si se desea
                // una relevancia específica en el título, pero puede ocultar resultados válidos.
                val serverResults = episodioRepository.buscarEpisodios(query) //

                // Opcional: Filtrar adicionalmente por título en el cliente.
                // val filteredByTitleResults = serverResults.filter { episodio ->
                // episodio.title.contains(query, ignoreCase = true)
                // }

                if (serverResults.isNotEmpty()) {
                    _searchResults.value = serverResults
                } else {
                    _searchResults.value = emptyList()
                    _searchError.value = "No se encontraron episodios para \"$query\"."
                }

            } catch (e: NoInternetException) {
                _searchError.value = e.message ?: "Sin conexión a internet."
                _searchResults.value = emptyList()
            } catch (e: ServerErrorException) {
                _searchError.value = e.userFriendlyMessage
                _searchResults.value = emptyList()
            } catch (e: ApiException) {
                _searchError.value = e.message ?: "Error de API durante la búsqueda."
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                _searchError.value = "Ocurrió un error al realizar la búsqueda."
                _searchResults.value = emptyList()
                // Considerar registrar `e` en un sistema de monitoreo.
            } finally {
                _isSearching.value = false
            }
        }
    }

    /** Limpia el texto de búsqueda actual y, consecuentemente, los resultados. */
    fun clearSearch() {
        _searchText.value = ""
        // El colector de _searchText se encargará de limpiar el resto.
    }

    /** Permite reintentar la última búsqueda realizada si falló anteriormente. */
    fun retrySearch() {
        val currentQuery = _searchText.value
        if (currentQuery.length >= MIN_QUERY_LENGTH) {
            performSearch(currentQuery)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel() // Asegurar que el job se cancela si el ViewModel se destruye.
    }
}