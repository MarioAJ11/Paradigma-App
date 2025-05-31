package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

/**
 * ViewModel para mi pantalla de búsqueda de episodios.
 * Gestiono el estado del texto de búsqueda, los resultados, el estado de carga
 * y los posibles errores. La búsqueda se activa cuando el usuario introduce al menos 3 caracteres
 * y se realiza un debounce para no sobrecargar con peticiones.
 * Los resultados del servidor se filtran adicionalmente en el cliente para asegurar que el término
 * de búsqueda esté presente en el título del episodio.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    // Ahora dependo de la abstracción del repositorio de episodios.
    private val episodioRepository: EpisodioRepository
    // Si necesitara SavedStateHandle:
    // private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Texto actual introducido por el usuario en la barra de búsqueda.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // Lista de episodios que coinciden con la búsqueda.
    private val _searchResults = MutableStateFlow<List<Episodio>>(emptyList())
    val searchResults: StateFlow<List<Episodio>> = _searchResults.asStateFlow()

    // Indica si una búsqueda está actualmente en progreso.
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Mensaje de error si la búsqueda falla o no produce resultados significativos.
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Job para la corutina de búsqueda, para poder cancelarla si el texto cambia.
    private var searchJob: Job? = null

    init {
        // Observo los cambios en el texto de búsqueda.
        viewModelScope.launch {
            _searchText
                .debounce(400) // Espero 400ms después de la última entrada para evitar búsquedas excesivas.
                .distinctUntilChanged() // Solo busco si el texto realmente ha cambiado.
                .collectLatest { query -> // Uso collectLatest para cancelar búsquedas previas si la query cambia rápido.
                    if (query.length > 2) { // Solo inicio la búsqueda si la query tiene más de 2 caracteres.
                        performSearch(query)
                    } else {
                        // Si la query es muy corta, limpio los resultados y el estado.
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        // Limpio el error solo si era un error de "no encontrado" o no había error,
                        // para no borrar errores de red/servidor persistentes.
                        if (_searchError.value?.startsWith("No se encontraron") == true ||
                            _searchError.value?.startsWith("Ningún título coincide") == true ||
                            _searchError.value == null) {
                            _searchError.value = null
                        }
                        searchJob?.cancel() // Cancelo cualquier búsqueda en curso.
                    }
                }
        }
    }

    /**
     * Se llama cuando el texto en la UI de búsqueda cambia.
     * @param query El nuevo texto de búsqueda.
     */
    fun onSearchTextChanged(query: String) {
        _searchText.value = query
    }

    // Realiza la búsqueda de episodios.
    private fun performSearch(query: String) {
        searchJob?.cancel() // Cancelo cualquier búsqueda anterior.
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null // Limpio errores al iniciar una nueva búsqueda.
            _searchResults.value = emptyList() // Limpio resultados anteriores.
            try {
                // Un pequeño delay opcional para mejorar la percepción de la UI si la respuesta es muy rápida.
                // delay(200)

                // Obtengo los resultados del servidor usando la interfaz del repositorio.
                val serverResults = episodioRepository.buscarEpisodios(query)

                // Filtro los resultados del servidor localmente para asegurar que el término de búsqueda
                // está contenido en el título del episodio (ignorando mayúsculas/minúsculas).
                val filteredByTitleResults = serverResults.filter { episodio ->
                    episodio.title.contains(query, ignoreCase = true)
                }

                if (filteredByTitleResults.isNotEmpty()) {
                    // Si encuentro episodios cuyo título coincide, los muestro.
                    _searchResults.value = filteredByTitleResults
                } else if (serverResults.isNotEmpty()) {
                    // Si no hay coincidencias de título, pero el servidor devolvió resultados
                    // (quizás el término estaba en el contenido), muestro todos los resultados del servidor.
                    _searchResults.value = serverResults
                    _searchError.value = "Ningún título coincide con \"$query\". Mostrando ${serverResults.size} resultados relacionados."
                } else {
                    // Si no se encontró nada, ni en el servidor ni por filtro de título.
                    _searchResults.value = emptyList()
                    _searchError.value = "No se encontraron episodios para \"$query\"."
                }

            } catch (e: NoInternetException) {
                _searchError.value = e.message ?: "Sin conexión a internet. Revisa tu conexión."
                _searchResults.value = emptyList()
            } catch (e: ServerErrorException) {
                _searchError.value = e.userFriendlyMessage
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                _searchError.value = "Ocurrió un error al realizar la búsqueda."
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false // Termino el estado de búsqueda.
            }
        }
    }

    /**
     * Limpia el texto de búsqueda actual y, consecuentemente, los resultados.
     */
    fun clearSearch() {
        _searchText.value = ""
        // El colector de _searchText en init se encargará de limpiar el resto.
    }

    /**
     * Permite reintentar la última búsqueda realizada.
     * Útil si hubo un error de red/servidor.
     */
    fun retrySearch() {
        val currentQuery = _searchText.value
        if (currentQuery.length > 2) { // Solo reintento si la query es válida.
            performSearch(currentQuery)
        }
    }

    // Cancelo el job de búsqueda si el ViewModel se destruye.
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}