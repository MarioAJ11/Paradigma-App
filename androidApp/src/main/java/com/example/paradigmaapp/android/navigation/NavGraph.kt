package com.example.paradigmaapp.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.paradigmaapp.android.screens.DownloadedEpisodioScreen
import com.example.paradigmaapp.android.screens.EpisodeDetailScreen
import com.example.paradigmaapp.android.screens.HomeScreen
import com.example.paradigmaapp.android.screens.OnGoingEpisodioScreen
import com.example.paradigmaapp.android.screens.ProgramaScreen
import com.example.paradigmaapp.android.screens.QueueScreen
import com.example.paradigmaapp.android.screens.SearchScreen
import com.example.paradigmaapp.android.screens.SettingsScreen
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.EpisodeDetailViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.OnGoingEpisodioViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Define el grafo de navegación principal de la aplicación.
 * Este composable actúa como el orquestador central de la UI, gestionando:
 * - La estructura de la pantalla con una UI de reproductor flotante.
 * - La navegación entre las diferentes pantallas a través de un [NavHost].
 * - La recolección de estado de los ViewModels para actualizar la UI.
 * - La inyección de dependencias (ViewModels) en cada pantalla que las necesite.
 *
 * @param navController El [NavHostController] para la navegación.
 * @param viewModelFactory La factoría para crear instancias de ViewModels con dependencias.
 * @param mainViewModel Instancia del [MainViewModel] global.
 * @param searchViewModel Instancia del [SearchViewModel] para la búsqueda.
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
    // Recolecta los estados de los ViewModels. La UI se recompondrá cuando estos estados cambien.
    val coroutineScope = rememberCoroutineScope()
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()
    val volumeFromViewModel by mainViewModel.currentVolume.collectAsState()
    val andainaRadioInfo by mainViewModel.andainaRadioInfo.collectAsState()
    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()

    // Obtiene instancias de los ViewModels que se pasarán a las pantallas.
    val queueViewModel: QueueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel: DownloadedEpisodioViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel: OnGoingEpisodioViewModel = mainViewModel.onGoingViewModel
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    // Contenedor para la hoja de control de volumen que se desliza desde abajo.
    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState,
        sheetContent = {
            VolumeControl(
                currentVolume = volumeFromViewModel,
                onVolumeChanged = { newVolume -> mainViewModel.setVolume(newVolume) }
            )
        },
        sheetPeekHeight = 0.dp,
    ) {
        // Usamos un Box para superponer la barra inferior sobre el contenido de la pantalla.
        Box(modifier = Modifier.fillMaxSize()) {

            // El NavHost contiene todas las pantallas de la app.
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Ruta para la pantalla principal (Home).
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre -> navController.navigate(Screen.Programa.createRoute(progId, progNombre)) }
                    )
                }

                // Ruta para la pantalla de detalles de un Programa.
                composable(
                    route = Screen.Programa.route,
                    arguments = listOf(navArgument("programaId") { type = NavType.IntType }, navArgument("programaNombre") { type = NavType.StringType })
                ) { navBackStackEntry ->
                    val programaViewModel: ProgramaViewModel = viewModel(key = "programa_vm_${navBackStackEntry.arguments?.getInt("programaId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory)
                    ProgramaScreen(programaViewModel, mainViewModel, queueViewModel, downloadedViewModel, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }

                // Ruta para la pantalla de Búsqueda.
                composable(Screen.Search.route) {
                    SearchScreen(
                        searchViewModel = searchViewModel,
                        mainViewModel = mainViewModel, // Pasamos mainViewModel para el estado de carga
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { mainViewModel.selectEpisode(it) },
                        onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Ruta para la pantalla de Descargas.
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen(
                        downloadedEpisodioViewModel = downloadedViewModel,
                        mainViewModel = mainViewModel, // Pasamos mainViewModel para el estado de carga
                        queueViewModel = queueViewModel,
                        onEpisodeSelected = { mainViewModel.selectEpisode(it) },
                        onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Ruta para la pantalla de Cola de Reproducción.
                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        mainViewModel = mainViewModel, // Pasamos mainViewModel para el estado de carga
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { mainViewModel.selectEpisode(it) },
                        onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Ruta para la pantalla de "Seguir Escuchando".
                composable(Screen.OnGoing.route) {
                    OnGoingEpisodioScreen(
                        onGoingEpisodioViewModel = onGoingViewModel,
                        mainViewModel = mainViewModel, // Pasamos mainViewModel para el estado de carga
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { mainViewModel.selectEpisode(it) },
                        onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Ruta para la pantalla de detalles de un Episodio.
                composable(
                    route = Screen.EpisodeDetail.route,
                    arguments = listOf(navArgument("episodeId") { type = NavType.IntType })
                ) { navBackStackEntry ->
                    val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(key = "episode_detail_vm_${navBackStackEntry.arguments?.getInt("episodeId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory)
                    EpisodeDetailScreen(episodeDetailViewModel, mainViewModel, queueViewModel, downloadedViewModel, onBackClick = { navController.popBackStack() })
                }

                // Ruta para la pantalla de Ajustes.
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsViewModel = settingsViewModel, onBackClick = { navController.popBackStack() })
                }
            }

            // La barra inferior (reproductor y navegación) se dibuja ENCIMA del NavHost.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                // Reproductor de audio global.
                AudioPlayer(
                    activePlayer = if (currentPlayingEpisode != null) mainViewModel.podcastExoPlayer else mainViewModel.andainaStreamPlayer.exoPlayer,
                    currentEpisode = currentPlayingEpisode,
                    andainaRadioInfo = andainaRadioInfo,
                    isPlayingGeneral = isPlayingGeneral,
                    episodeProgress = episodeProgress,
                    onProgressChange = { newProgress -> mainViewModel.seekEpisodeTo(newProgress) },
                    isAndainaStreamActive = isAndainaStreamActive,
                    isAndainaPlaying = isAndainaPlaying,
                    onPlayPauseClick = { mainViewModel.onPlayerPlayPauseClick() },
                    onPlayStreamingClick = { mainViewModel.toggleAndainaStreamActive() },
                    onEpisodeInfoClick = { episodio -> navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id)) },
                    onVolumeIconClick = {
                        coroutineScope.launch {
                            if (volumeBottomSheetScaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                                volumeBottomSheetScaffoldState.bottomSheetState.partialExpand()
                            } else {
                                volumeBottomSheetScaffoldState.bottomSheetState.expand()
                            }
                        }
                    }
                )

                // Barra de navegación inferior.
                BottomNavigationBar(
                    navController = navController
                )
            }
        }
    }
}