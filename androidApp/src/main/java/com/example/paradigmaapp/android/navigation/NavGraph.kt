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

/**
 * Define el grafo de navegación principal de la aplicación.
 * Este Composable configura el `NavHost` para la navegación entre pantallas.
 * Utiliza un `BottomSheetScaffold` para mostrar el `VolumeControl` en un panel
 * que se puede expandir o colapsar desde la parte inferior.
 *
 * @param navController El [NavHostController] que gestiona la pila de navegación.
 * @param viewModelFactory La factoría para crear instancias de [ViewModel].
 * @param mainViewModel El [MainViewModel] principal para el estado global.
 * @param searchViewModel El [SearchViewModel] para la búsqueda.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {

    val coroutineScope = rememberCoroutineScope()
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()
    val volumeFromViewModel by mainViewModel.currentVolume.collectAsState()
    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val queueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel = mainViewModel.onGoingViewModel

    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState,
        sheetContent = {
            VolumeControl(
                onVolumeChanged = { newVolume ->
                    mainViewModel.setVolume(newVolume)
                },
                currentVolume = volumeFromViewModel
            )
        },
        sheetPeekHeight = 0.dp,
        containerColor = MaterialTheme.colorScheme.background
    )  { paddingValuesDelBottomSheet ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesDelBottomSheet)
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                // Definición de la pantalla de Inicio (HomeScreen)
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre ->
                            navController.navigate(Screen.Programa.createRoute(progId, progNombre))
                        },
                    )
                }

                // Definición de la pantalla de Programa (ProgramaScreen)
                composable(
                    route = Screen.Programa.route,
                    arguments = listOf(
                        navArgument("programaId") { type = NavType.IntType },
                        navArgument("programaNombre") { type = NavType.StringType }
                    )
                ) { navBackStackEntry ->
                    val arguments = navBackStackEntry.arguments
                    val programaIdFromArgs = arguments?.getInt("programaId") ?: -1
                    val programaViewModel: ProgramaViewModel = viewModel(
                        key = "programa_vm_$programaIdFromArgs",
                        viewModelStoreOwner = navBackStackEntry, // Usar navBackStackEntry como owner
                        factory = viewModelFactory
                    )
                    ProgramaScreen(
                        programaViewModel = programaViewModel,
                        mainViewModel = mainViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
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
                            mainViewModel.selectEpisode(episodio)
                        },
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Definición de la pantalla de Descargas (DownloadedEpisodioScreen)
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen(
                        downloadedEpisodioViewModel = downloadedViewModel,
                        queueViewModel = queueViewModel,
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // Definición de la pantalla de Cola (QueueScreen)
                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
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
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Definición de la pantalla de Detalles del Episodio (EpisodeDetailScreen)
                composable(
                    route = Screen.EpisodeDetail.route,
                    arguments = listOf(
                        navArgument("episodeId") { type = NavType.IntType }
                    )
                ) { navBackStackEntry ->
                    val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(
                        key = "episode_detail_vm_${navBackStackEntry.arguments?.getInt("episodeId")}",
                        viewModelStoreOwner = navBackStackEntry, // Usar navBackStackEntry como owner
                        factory = viewModelFactory
                    )
                    EpisodeDetailScreen(
                        episodeDetailViewModel = episodeDetailViewModel,
                        mainViewModel = mainViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // NUEVA NAVEGACIÓN PARA LA PANTALLA DE AJUSTES
                composable(Screen.Settings.route) { navBackStackEntry ->
                    val settingsViewModel: SettingsViewModel = viewModel(
                        key = "settings_vm", // Clave única para el ViewModel de Ajustes
                        viewModelStoreOwner = navBackStackEntry, // Usar navBackStackEntry como owner
                        factory = viewModelFactory
                    )
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

            } // Fin del NavHost

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
                    navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                },
                onVolumeIconClick = {
                    coroutineScope.launch {
                        val sheetState = volumeBottomSheetScaffoldState.bottomSheetState
                        if (sheetState.currentValue == SheetValue.Expanded) {
                            sheetState.partialExpand()
                        } else {
                            sheetState.expand()
                        }
                    }
                }
            )

            BottomNavigationBar(
                navController = navController,
                onSearchClick = {
                    if (navController.currentDestination?.route != Screen.Search.route) {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onOnGoingClick = {
                    if (navController.currentDestination?.route != Screen.OnGoing.route) {
                        navController.navigate(Screen.OnGoing.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onDownloadedClick = {
                    if (navController.currentDestination?.route != Screen.Downloads.route) {
                        navController.navigate(Screen.Downloads.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onQueueClick = {
                    if (navController.currentDestination?.route != Screen.Queue.route) {
                        navController.navigate(Screen.Queue.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onSettingsClick = {
                    // Navegar a la nueva pantalla de Ajustes
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}