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
import com.example.paradigmaapp.android.ui.ErrorView // Importar ErrorView
import com.example.paradigmaapp.android.ui.ErrorType // Importar ErrorType
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val searchError by searchViewModel.searchError.collectAsState() // Ya existe

    val downloadedEpisodeIds by downloadedViewModel.downloadedEpisodeIds.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    focusManager.clearFocus()
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                searchError != null && searchResults.isEmpty() -> { // Mostrar ErrorView si hay un error y no hay resultados previos
                    val errorType = if (searchError!!.contains("internet", ignoreCase = true) || searchError!!.contains("conectar", ignoreCase = true)) {
                        ErrorType.NO_INTERNET
                    } else if (searchError!!.contains("No se encontraron", ignoreCase = true) || searchError!!.startsWith("No hay títulos que coincidan")) {
                        ErrorType.NO_RESULTS
                    }
                    else {
                        ErrorType.GENERAL_SERVER_ERROR
                    }
                    ErrorView(
                        message = searchError!!,
                        errorType = errorType,
                        // Solo mostrar reintentar si no es un error de "no resultados" simple
                        onRetry = if (errorType != ErrorType.NO_RESULTS) { { searchViewModel.retrySearch() } } else null
                    )
                }
                searchText.length > 2 && searchResults.isEmpty() && !isSearching && searchError == null -> {
                    ErrorView( // Mensaje cuando la búsqueda finalizó sin resultados y sin error de API
                        message = "No se encontraron episodios para \"$searchText\".",
                        errorType = ErrorType.NO_RESULTS
                    )
                }
                searchResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
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
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
                searchText.length <= 2 && !isSearching -> { // Mensaje para que el usuario escriba más
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                                text = "Escribe al menos 3 caracteres para buscar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                else -> { // Estado inicial o cuando la query es muy corta y no hay error
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Buscar",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}