package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search // Icono de búsqueda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodioListItem // Importa el item de episodio actualizado
import com.example.paradigmaapp.android.ui.SearchBar // Importa tu barra de búsqueda
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.launch

/**
 * Pantalla para la búsqueda de episodios.
 * Muestra una barra de búsqueda y los resultados correspondientes.
 * Permite realizar acciones sobre los episodios encontrados (reproducir, añadir a cola, descargar, etc.).
 *
 * @param searchViewModel ViewModel que gestiona la lógica y el estado de la búsqueda.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción.
 * @param downloadedViewModel ViewModel para interactuar con las descargas.
 * @param onEpisodeSelected Lambda que se invoca cuando se selecciona un episodio de los resultados para reproducirlo.
 * @param onBackClick Lambda para manejar la acción de retroceso (cerrar la pantalla de búsqueda).
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onEpisodeSelected: (Episodio) -> Unit, // Esta es la acción principal de clic (reproducir)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .statusBarsPadding(), // Padding para la barra de estado
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    focusManager.clearFocus() // Oculta el teclado
                    onBackClick() // Acción de volver
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, // Icono de flecha hacia atrás
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
                        .padding(end = 8.dp), // Espacio a la derecha de la barra de búsqueda
                    label = "Buscar episodios..." // Etiqueta para la barra de búsqueda
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Padding del Scaffold
                .background(MaterialTheme.colorScheme.background) // Color de fondo
        ) {
            when {
                isSearching -> {
                    // Indicador de carga centrado
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                searchError != null && searchResults.isEmpty() -> {
                    // Mensaje de error o "no se encontraron resultados"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = searchError!!, // El mensaje de error viene del ViewModel
                            color = if (searchResults.isEmpty() && searchText.length > 2) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                searchText.length > 2 && searchResults.isEmpty() && !isSearching && searchError == null -> {
                    // Mensaje cuando la búsqueda finalizó sin resultados y sin error de API
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No se encontraron resultados para \"$searchText\".",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                searchResults.isNotEmpty() -> {
                    // Lista de resultados de búsqueda
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp) // Padding vertical para la lista
                    ) {
                        items(searchResults, key = { episodio -> episodio.id }) { episodio ->
                            EpisodioListItem(
                                episodio = episodio,
                                onPlayEpisode = { onEpisodeSelected(it) }, // Acción de reproducir
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                                onDownloadEpisode = { ep, onMsgCallback ->
                                    downloadedViewModel.downloadEpisodio(ep) { message ->
                                        onMsgCallback(message) // Pasa el mensaje
                                        coroutineScope.launch { // Muestra el Snackbar
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                                onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisodio(it) },
                                isDownloaded = downloadedEpisodeIds.contains(episodio.id),
                                isInQueue = queueEpisodeIds.contains(episodio.id),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp) // Espacio entre items
                            )
                        }
                    }
                }
                searchText.length <= 2 && !isSearching -> {
                    // Mensaje para que el usuario escriba más caracteres
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { // Para centrar icono y texto
                            Icon(
                                Icons.Filled.Search, // Icono de búsqueda
                                contentDescription = "Buscar",
                                modifier = Modifier.size(60.dp), // Tamaño del icono
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Color del icono
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
                else -> {
                    // Estado inicial o cuando la query es muy corta y no hay error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Search, // Icono de búsqueda grande
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