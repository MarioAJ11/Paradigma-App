package com.example.paradigmaapp.android.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
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
import com.example.paradigmaapp.android.screens.*
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch

/**
 * Define el grafo de navegación para la aplicación principal, una vez que el usuario
 * ha completado el onboarding.
 * Gestiona la estructura de la UI con el reproductor y la barra de navegación persistentes,
 * y define todas las rutas navegables (pantallas) de la app.
 *
 * @author Mario Alguacil Juárez
 * @param navController El controlador de navegación para moverse entre pantallas.
 * @param viewModelFactory La factoría para crear instancias de ViewModels.
 * @param mainViewModel El ViewModel principal que gestiona el estado global.
 * @param searchViewModel El ViewModel para la funcionalidad de búsqueda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    // Recolecta los estados del ViewModel para que la UI se actualice en tiempo real.
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
    val isFullScreenPlayerVisible by mainViewModel.isFullScreenPlayerVisible.collectAsState()

    // Contenedor principal que permite superponer vistas.
    Box(modifier = Modifier.fillMaxSize()) {
        // Hoja inferior para el control de volumen.
        BottomSheetScaffold(
            scaffoldState = volumeBottomSheetScaffoldState,
            sheetContent = {
                VolumeControl(
                    currentVolume = volumeFromViewModel,
                    onVolumeChanged = { newVolume -> mainViewModel.setVolume(newVolume) }
                )
            },
            sheetPeekHeight = 0.dp, // La hoja está oculta por defecto.
        ) {
            // Contenedor para el contenido principal y la barra inferior.
            Box(modifier = Modifier.fillMaxSize()) {
                // Host de navegación que gestiona el cambio entre pantallas.
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route, // La app siempre empieza en Home.
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Define cada una de las pantallas (rutas) de la aplicación.
                    composable(Screen.Home.route) {
                        HomeScreen(
                            mainViewModel = mainViewModel,
                            onProgramaSelected = { progId, progNombre -> navController.navigate(Screen.Programa.createRoute(progId, progNombre)) }
                        )
                    }
                    composable(route = Screen.Programa.route, arguments = listOf(navArgument("programaId") { type = NavType.IntType }, navArgument("programaNombre") { type = NavType.StringType })) { navBackStackEntry -> val programaViewModel: ProgramaViewModel = viewModel(key = "programa_vm_${navBackStackEntry.arguments?.getInt("programaId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); ProgramaScreen(programaViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Search.route) { SearchScreen(searchViewModel = searchViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Downloads.route) { DownloadedEpisodioScreen(downloadedEpisodioViewModel = mainViewModel.downloadedViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Queue.route) { QueueScreen(queueViewModel = mainViewModel.queueViewModel, mainViewModel = mainViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.OnGoing.route) { OnGoingEpisodioScreen(onGoingEpisodioViewModel = mainViewModel.onGoingViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(route = Screen.EpisodeDetail.route, arguments = listOf(navArgument("episodeId") { type = NavType.IntType })) { navBackStackEntry -> val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(key = "episode_detail_vm_${navBackStackEntry.arguments?.getInt("episodeId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); EpisodeDetailScreen(episodeDetailViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Settings.route) { SettingsScreen(settingsViewModel = viewModel(factory = viewModelFactory), onBackClick = { navController.popBackStack() }) }
                }

                // UI persistente en la parte inferior (reproductor y menú).
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
                        onEpisodeInfoClick = { mainViewModel.toggleFullScreenPlayer() },
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

        // El reproductor a pantalla completa se muestra por encima de todo cuando está visible.
        AnimatedVisibility(
            visible = isFullScreenPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullScreenPlayerScreen(
                mainViewModel = mainViewModel,
                onBackClick = { mainViewModel.toggleFullScreenPlayer() }
            )
        }
    }
}