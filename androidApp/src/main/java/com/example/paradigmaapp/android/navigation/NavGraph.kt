package com.example.paradigmaapp.android.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.screens.*
import com.example.paradigmaapp.android.ui.SettingsScreen
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Define el grafo de navegación principal de la aplicación.
 * Incluye el BottomSheetScaffold, AudioPlayer global, y gestiona la instanciación
 * de ViewModels específicos de pantalla usando la ViewModelFactory.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: ViewModelFactory, // Factory para crear ViewModels
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    // Estados del reproductor (observados desde MainViewModel)
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()

    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Acceder a los ViewModels que MainViewModel ya tiene instancias
    // Estos ViewModels son gestionados por MainViewModel o creados por la factory según sea necesario.
    val queueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel = mainViewModel.onGoingViewModel

    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState, // Estado para el BottomSheet del volumen
        sheetContent = { // Contenido del BottomSheet (control de volumen)
            VolumeControl(
                player = if (currentPlayingEpisode != null) mainViewModel.podcastExoPlayer else mainViewModel.andainaStreamPlayer.exoPlayer,
                onVolumeChanged = { newVolume ->
                    if (currentPlayingEpisode != null) {
                        mainViewModel.podcastExoPlayer.volume = newVolume
                    } else {
                        mainViewModel.andainaStreamPlayer.exoPlayer?.volume = newVolume
                    }
                },
                currentVolume = if (currentPlayingEpisode != null) mainViewModel.podcastExoPlayer.volume else mainViewModel.andainaStreamPlayer.exoPlayer?.volume ?: 0f,
                onBluetoothDeviceSelected = { deviceName ->
                    coroutineScope.launch { volumeBottomSheetScaffoldState.bottomSheetState.partialExpand() }
                }
            )
        },
        sheetPeekHeight = 0.dp,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValuesDelBottomSheet ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesDelBottomSheet)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // NavHost para gestionar la navegación entre pantallas
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route, // Pantalla inicial
                modifier = Modifier.weight(1f) // El NavHost ocupa el espacio principal
            ) {
                // Definición de la pantalla de Inicio (HomeScreen)
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre ->
                            // Navega a la pantalla del programa con su ID y nombre
                            navController.navigate(Screen.Programa.createRoute(progId, progNombre))
                        },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) } // Navega a la pantalla de búsqueda
                    )
                }

                // Definición de la pantalla de Programa (ProgramaScreen)
                composable(
                    route = Screen.Programa.route,
                    arguments = listOf( // Argumentos que espera esta ruta
                        navArgument("programaId") { type = NavType.IntType },
                        navArgument("programaNombre") { type = NavType.StringType }
                    )
                ) { navBackStackEntry ->
                    val arguments = navBackStackEntry.arguments
                    val programaIdFromArgs = arguments?.getInt("programaId") ?: -1 // Obtiene el ID del programa

                    // Crea el ProgramaViewModel con una clave única y el navBackStackEntry como owner
                    val programaViewModel: ProgramaViewModel = viewModel(
                        key = "programa_vm_$programaIdFromArgs", // Clave única para el ViewModel
                        viewModelStoreOwner = navBackStackEntry, // Asocia el ViewModel al ciclo de vida de esta entrada de navegación
                        factory = viewModelFactory // Usa la factory global
                    )

                    ProgramaScreen(
                        programaViewModel = programaViewModel,
                        mainViewModel = mainViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Definición de la pantalla de Búsqueda (SearchScreen)
                composable(Screen.Search.route) {
                    SearchScreen(
                        searchViewModel = searchViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio ->
                            mainViewModel.selectEpisode(episodio) // Selecciona el episodio para reproducción
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Definición de la pantalla de Descargas (DownloadedEpisodioScreen)
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen(
                        downloadedEpisodioViewModel = downloadedViewModel,
                        queueViewModel = queueViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Definición de la pantalla de Cola (QueueScreen)
                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Definición de la pantalla de "Seguir Escuchando" (OnGoingEpisodioScreen)
                composable(Screen.OnGoing.route) {
                    OnGoingEpisodioScreen(
                        onGoingEpisodioViewModel = onGoingViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Reproductor de Audio Global
            AudioPlayer(
                activePlayer = if (currentPlayingEpisode != null) mainViewModel.podcastExoPlayer else mainViewModel.andainaStreamPlayer.exoPlayer,
                currentEpisode = currentPlayingEpisode,
                isPlayingGeneral = isPlayingGeneral,
                episodeProgress = episodeProgress,
                onProgressChange = { newProgress -> mainViewModel.seekEpisodeTo(newProgress) },
                isAndainaStreamActive = isAndainaStreamActive,
                isAndainaPlaying = isAndainaPlaying,
                onPlayPauseClick = { mainViewModel.onPlayerPlayPauseClick() },
                onPlayStreamingClick = { mainViewModel.toggleAndainaStreamActive() },
                onEpisodeInfoClick = { episodio ->
                    Timber.d("Info click para episodio: ${episodio.title}")
                    navController.navigate("episode_detail_screen/${episodio.id}")
                },
                onVolumeIconClick = { // Acción para mostrar el control de volumen (BottomSheet)
                    coroutineScope.launch {
                        if (volumeBottomSheetScaffoldState.bottomSheetState.isVisible) {
                            volumeBottomSheetScaffoldState.bottomSheetState.hide()
                        } else {
                            volumeBottomSheetScaffoldState.bottomSheetState.expand()
                        }
                    }
                }
            )

            // Barra de Navegación Inferior
            BottomNavigationBar(
                navController = navController,
                onSearchClick = { // Navega a la pantalla de búsqueda
                    if (navController.currentDestination?.route != Screen.Search.route) {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                },
                onOnGoingClick = { navController.navigate(Screen.OnGoing.route) { popUpTo(Screen.Home.route); launchSingleTop = true } },
                onDownloadedClick = { navController.navigate(Screen.Downloads.route) { popUpTo(Screen.Home.route); launchSingleTop = true } },
                onQueueClick = { navController.navigate(Screen.Queue.route) { popUpTo(Screen.Home.route); launchSingleTop = true } },
                onSettingsClick = { showSettingsDialog = true } // Muestra el diálogo de ajustes
            )
        }
    }

    // Diálogo de Ajustes (si showSettingsDialog es true)
    if (showSettingsDialog) {
        SettingsScreen(
            onDismissRequest = { showSettingsDialog = false }, // Acción para cerrar el diálogo
            isStreamActive = isAndainaStreamActive,
            onStreamActiveChanged = {
                mainViewModel.toggleAndainaStreamActive() // Cambia el estado del stream
                showSettingsDialog = false // Cierra el diálogo después del cambio
            }
        )
    }
}