package com.example.paradigmaapp.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import io.ktor.http.contentRangeHeaderValue
import kotlinx.coroutines.launch

/**
 * Define el grafo de navegación principal de la aplicación utilizando Jetpack Compose Navigation.
 *
 * @param navController El [NavHostController] principal para la navegación.
 * @param viewModelFactory La factoría [ViewModelFactory] para crear instancias de ViewModels con dependencias.
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
    val coroutineScope = rememberCoroutineScope()
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()
    val volumeFromViewModel by mainViewModel.currentVolume.collectAsState()
    val andainaRadioInfo by mainViewModel.andainaRadioInfo.collectAsState() // Estado de la info del stream
    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val queueViewModel: QueueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel: DownloadedEpisodioViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel: OnGoingEpisodioViewModel = mainViewModel.onGoingViewModel
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState,
        sheetContent = {
            VolumeControl(
                currentVolume = volumeFromViewModel,
                onVolumeChanged = { newVolume -> mainViewModel.setVolume(newVolume) }
            )
        },
        sheetContentColor = Color.Red,
        sheetPeekHeight = 0.dp,
    ) { paddingValuesFromBottomSheet ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromBottomSheet)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .weight(1f)
            ) {
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

/** Función de utilidad para la navegación de la barra inferior.
 * Comprueba si la ruta actual es diferente de la ruta de destino antes de navegar.
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