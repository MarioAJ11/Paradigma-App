package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.NoInternetException
import com.example.paradigmaapp.exception.ServerErrorException
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

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
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        // Limpiar error si la query es muy corta, pero no si ya hay un error de red/servidor
                        if (_searchError.value?.startsWith("No se encontraron") == true || _searchError.value == null) {
                            _searchError.value = null
                        }
                        searchJob?.cancel() // Cancelar cualquier búsqueda en curso
                    }
                }
        }
    }

    fun onSearchTextChanged(query: String) {
        _searchText.value = query
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null // Limpiar error al iniciar nueva búsqueda
            _searchResults.value = emptyList() // Limpiar resultados anteriores inmediatamente
            try {
                Timber.d("SearchViewModel: Iniciando búsqueda para: '$query'")
                // Pequeño delay para que el usuario vea el indicador de carga si la respuesta es muy rápida
                delay(200)

                // Paso 1: Obtener resultados del servidor (búsqueda general de WordPress)
                val serverResults = wordpressService.buscarEpisodios(query)
                Timber.d("SearchViewModel: Resultados del servidor (antes de cualquier filtro local): ${serverResults.size} para query '$query'")
                serverResults.take(5).forEach { ep ->
                    Timber.d("SearchViewModel: Título del servidor ejemplo: '${ep.title}' (ID: ${ep.id})")
                }

                // Paso 2: Filtrar en el cliente para frase exacta en el título (ignorando mayúsculas/minúsculas)
                val filteredByTitleResults = serverResults.filter { episodio ->
                    // episodio.title ya es el título renderizado y decodificado de entidades HTML
                    episodio.title.contains(query, ignoreCase = true)
                }
                Timber.d("SearchViewModel: Resultados filtrados por título localmente: ${filteredByTitleResults.size}")

                if (filteredByTitleResults.isNotEmpty()) {
                    _searchResults.value = filteredByTitleResults
                    // _searchError.value = null // Ya se limpió al inicio
                } else if (serverResults.isNotEmpty()) {
                    // No se encontró por título, pero el servidor devolvió algo (quizás por contenido)
                    _searchResults.value = serverResults // Mostrar todos los resultados del servidor
                    _searchError.value = "Ningún título coincide con \"$query\". Mostrando ${serverResults.size} resultados relacionados."
                    Timber.d("SearchViewModel: Ningún título coincidió. Mostrando resultados generales del servidor.")
                } else {
                    // Ni el filtro por título ni el servidor encontraron nada
                    _searchResults.value = emptyList()
                    _searchError.value = "No se encontraron episodios para \"$query\"."
                    Timber.d("SearchViewModel: No se encontraron resultados ni en servidor ni por filtro de título.")
                }

            } catch (e: NoInternetException) {
                Timber.e(e, "SearchViewModel: Error de red durante la búsqueda para query '$query'")
                _searchError.value = e.message ?: "Sin conexión a internet. Revisa tu conexión."
                _searchResults.value = emptyList()
            } catch (e: ServerErrorException) {
                Timber.e(e, "SearchViewModel: Error de servidor durante la búsqueda para query '$query'")
                _searchError.value = e.userFriendlyMessage
                _searchResults.value = emptyList()
            } catch (e: Exception) { // Otras excepciones
                Timber.e(e, "SearchViewModel: Error inesperado durante la búsqueda para query '$query'")
                _searchError.value = "Ocurrió un error al realizar la búsqueda."
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchText.value = ""
        // El colector de _searchText se encargará de limpiar los resultados y el estado.
    }

    fun retrySearch() {
        val currentQuery = _searchText.value
        if (currentQuery.length > 2) {
            performSearch(currentQuery)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}