package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Pantalla que permite al usuario buscar episodios. Muestra resultados en tiempo real
 * y proporciona feedback de carga y error.
 *
 * @param searchViewModel ViewModel que gestiona la lógica de búsqueda.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param queueViewModel ViewModel para interactuar con la cola.
 * @param downloadedViewModel ViewModel para interactuar con las descargas.
 * @param onEpisodeSelected Lambda que se invoca al seleccionar un episodio para reproducir.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un episodio.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    mainViewModel: MainViewModel, // <-- PARÁMETRO AÑADIDO
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    // Estados de la búsqueda
    val searchText by searchViewModel.searchText.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchError by searchViewModel.searchError.collectAsState()

    // Estados globales
    val downloadedEpisodios by downloadedViewModel.downloadedEpisodios.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState() // <-- AÑADIDO

    // Controladores de UI y corutinas
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { focusManager.clearFocus(); onBackClick() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                }
                SearchBar(
                    searchText = searchText,
                    onSearchTextChanged = { query -> searchViewModel.onSearchTextChanged(query) },
                    onClearSearch = { searchViewModel.clearSearch() },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    label = "Buscar episodios..."
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)
        ) {
            when {
                // Estado: buscando...
                isSearching -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                }
                // Estado: error
                searchError != null && searchResults.isEmpty() -> {
                    val errorType = determineErrorType(searchError)
                    ErrorView(message = searchError!!, errorType = errorType, onRetry = if (errorType != ErrorType.NO_RESULTS) { { searchViewModel.retrySearch() } } else null)
                }
                // Estado: sin resultados
                searchText.length >= 3 && searchResults.isEmpty() && !isSearching && searchError == null -> {
                    ErrorView(message = "No se encontraron episodios para \"$searchText\".", errorType = ErrorType.NO_RESULTS)
                }
                // Estado: resultados encontrados
                searchResults.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)) {
                        items(searchResults, key = { it.id }) { episodio ->
                            val isLoading = episodio.id == preparingEpisodeId
                            EpisodioListItem(
                                episodio = episodio,
                                isLoading = isLoading, // <-- Pasando el estado de carga
                                onPlayEpisode = { onEpisodeSelected(it) },
                                onEpisodeLongClick = { onEpisodeLongClicked(it) },
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                                onDownloadEpisode = { ep, onMsgCallback ->
                                    downloadedViewModel.downloadEpisodio(ep) { message ->
                                        onMsgCallback(message)
                                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                },
                                onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                                isDownloaded = downloadedEpisodios.any { it.id == episodio.id },
                                isInQueue = queueEpisodeIds.contains(episodio.id),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                // Estado: esperando a que el usuario escriba más
                searchText.length < 3 && !isSearching -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Search, "Buscar", Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Text("Escribe al menos 3 caracteres para buscar.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        }
                    }
                }
                // Estado inicial
                else -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                        Icon(Icons.Filled.Search, "Pantalla de Búsqueda", Modifier.size(120.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

/** Determina el tipo de error para mostrar el icono adecuado.
 *
 *  @param errorMessage El mensaje de error a analizar.
 *  @return El tipo de error correspondiente.
 */
private fun determineErrorType(errorMessage: String?): ErrorType {
    return when {
        errorMessage == null -> ErrorType.GENERAL_SERVER_ERROR
        errorMessage.contains("internet", true) || errorMessage.contains("conectar", true) -> ErrorType.NO_INTERNET
        errorMessage.startsWith("No se encontraron", true) -> ErrorType.NO_RESULTS
        else -> ErrorType.GENERAL_SERVER_ERROR
    }
}