package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Pantalla que permite al usuario buscar episodios por un término de búsqueda.
 * Muestra una barra de búsqueda y una lista de resultados, gestionando los estados
 * de carga, error o ausencia de resultados.
 *
 * @param searchViewModel ViewModel que gestiona la lógica y el estado de la búsqueda.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción.
 * @param downloadedViewModel ViewModel para conocer el estado de descarga de los episodios.
 * @param onEpisodeSelected Lambda que se invoca cuando un episodio es seleccionado de los resultados.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un episodio.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit,
    onEpisodeLongClicked: (Episodio) -> Unit,
    onBackClick: () -> Unit
) {
    val searchText by searchViewModel.searchText.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchError by searchViewModel.searchError.collectAsState()

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Barra superior personalizada con botón de retroceso y SearchBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Padding para la barra de estado
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    focusManager.clearFocus() // Ocultar teclado al retroceder
                    onBackClick()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SearchBar(
                    searchText = searchText,
                    onSearchTextChanged = { query -> searchViewModel.onSearchTextChanged(query) },
                    onClearSearch = { searchViewModel.clearSearch() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    label = "Buscar episodios..."
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isSearching -> {
                    // Estado de carga: Muestra un indicador de progreso.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                // Error y sin resultados previos: Muestra ErrorView.
                searchError != null && searchResults.isEmpty() -> {
                    val errorType = determineErrorType(searchError) // Función auxiliar para determinar ErrorType
                    ErrorView(
                        message = searchError!!,
                        errorType = errorType,
                        onRetry = if (errorType != ErrorType.NO_RESULTS) { { searchViewModel.retrySearch() } } else null
                    )
                }
                // Búsqueda completada, sin resultados y sin error de API: Mensaje específico.
                searchText.length >= SearchViewModel.MIN_QUERY_LENGTH && searchResults.isEmpty() && !isSearching && searchError == null -> {
                    ErrorView(
                        message = "No se encontraron episodios para \"$searchText\".",
                        errorType = ErrorType.NO_RESULTS
                    )
                }
                // Resultados encontrados: Muestra la lista.
                searchResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        items(searchResults, key = { episodio -> episodio.id }) { episodio ->
                            EpisodioListItem(
                                episodio = episodio,
                                onPlayEpisode = { onEpisodeSelected(it) },
                                onEpisodeLongClick = { onEpisodeLongClicked(it) },
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                                onDownloadEpisode = { ep, onMsgCallback ->
                                    downloadedViewModel.downloadEpisodio(ep) { message ->
                                        onMsgCallback(message)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                                onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                                isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                                isInQueue = queueEpisodeIds.contains(episodio.id),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                // Texto de búsqueda demasiado corto: Mensaje instructivo.
                searchText.length < SearchViewModel.MIN_QUERY_LENGTH && !isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Buscar",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Escribe al menos ${SearchViewModel.MIN_QUERY_LENGTH} caracteres para buscar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Estado inicial o por defecto (ej. campo de búsqueda vacío al entrar).
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Search, // Icono grande de búsqueda
                            contentDescription = "Pantalla de Búsqueda",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Función auxiliar para determinar el [ErrorType] basado en el mensaje de error.
 * @param errorMessage El mensaje de error.
 * @return El [ErrorType] correspondiente.
 */
private fun determineErrorType(errorMessage: String?): ErrorType {
    return when {
        errorMessage == null -> ErrorType.GENERAL_SERVER_ERROR // Default si el mensaje es nulo
        errorMessage.contains("internet", ignoreCase = true) ||
                errorMessage.contains("conectar", ignoreCase = true) ||
                errorMessage.contains("red", ignoreCase = true) -> ErrorType.NO_INTERNET
        errorMessage.startsWith("No se encontraron", ignoreCase = true) ||
                errorMessage.startsWith("No hay títulos que coincidan", ignoreCase = true) -> ErrorType.NO_RESULTS
        else -> ErrorType.GENERAL_SERVER_ERROR
    }
}