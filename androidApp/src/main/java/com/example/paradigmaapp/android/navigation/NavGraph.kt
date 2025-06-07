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
 * - La estructura de la pantalla (con un Box para la UI flotante).
 * - La navegación entre las diferentes pantallas a través de un [NavHost].
 * - La recolección de estado de los ViewModels principales.
 * - La presentación de los componentes persistentes como el [AudioPlayer] y la [BottomNavigationBar].
 *
 * @param navController El [NavHostController] para la navegación. Por defecto, crea uno nuevo.
 * @param viewModelFactory La factoría para crear instancias de ViewModels con dependencias.
 * @param mainViewModel Instancia del [MainViewModel] global.
 * @param searchViewModel Instancia del [SearchViewModel] para la búsqueda.
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
        // Usamos un Box como contenedor principal para poder superponer la barra inferior
        // sobre el contenido de la pantalla.
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. El NavHost se coloca primero, ocupando todo el fondo.
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Aquí se definen todas las rutas/pantallas de la aplicación.
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre -> navController.navigate(Screen.Programa.createRoute(progId, progNombre)) }
                    )
                }
                composable(
                    route = Screen.Programa.route,
                    arguments = listOf(navArgument("programaId") { type = NavType.IntType }, navArgument("programaNombre") { type = NavType.StringType })
                ) { navBackStackEntry ->
                    val programaViewModel: ProgramaViewModel = viewModel(key = "programa_vm_${navBackStackEntry.arguments?.getInt("programaId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory)
                    ProgramaScreen(programaViewModel, mainViewModel, queueViewModel, downloadedViewModel, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }
                composable(Screen.Search.route) {
                    SearchScreen(searchViewModel, queueViewModel, downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen(downloadedViewModel, queueViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }
                composable(Screen.Queue.route) {
                    QueueScreen(queueViewModel, downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }
                composable(Screen.OnGoing.route) {
                    OnGoingEpisodioScreen(onGoingViewModel, queueViewModel, downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() })
                }
                composable(
                    route = Screen.EpisodeDetail.route,
                    arguments = listOf(navArgument("episodeId") { type = NavType.IntType })
                ) { navBackStackEntry ->
                    val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(key = "episode_detail_vm_${navBackStackEntry.arguments?.getInt("episodeId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory)
                    EpisodeDetailScreen(episodeDetailViewModel, mainViewModel, queueViewModel, downloadedViewModel, onBackClick = { navController.popBackStack() })
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsViewModel = settingsViewModel, onBackClick = { navController.popBackStack() })
                }
            }

            // 2. La barra inferior se coloca después, por lo que se dibuja ENCIMA del NavHost.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Se alinea en la parte inferior del Box.
                    .background(
                        // Se aplica un degradado para el efecto de "sombra".
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
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

                BottomNavigationBar(
                    navController = navController,
                    onSearchClick = { navigateToScreenIfDifferent(navController, Screen.Search.route) },
                    onOnGoingClick = { navigateToScreenIfDifferent(navController, Screen.OnGoing.route) },
                    onDownloadedClick = { navigateToScreenIfDifferent(navController, Screen.Downloads.route) },
                    onQueueClick = { navigateToScreenIfDifferent(navController, Screen.Queue.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
                )
            }
        }
    }
}

/** * Función de utilidad para la navegación de la barra inferior.
 * Comprueba si la ruta actual es diferente de la ruta de destino antes de navegar,
 * para evitar añadir la misma pantalla a la pila de navegación múltiples veces.
 * Gestiona la pila de retroceso para un comportamiento de navegación estándar en la barra inferior.
 *
 * @param navController El controlador de navegación.
 * @param route La ruta de destino.
 *
 * @author Mario Alguacil Juárez
 */
private fun navigateToScreenIfDifferent(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route != route) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}