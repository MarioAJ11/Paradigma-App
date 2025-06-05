package com.example.paradigmaapp.android.navigation

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
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch

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
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)


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
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre ->
                            navController.navigate(Screen.Programa.createRoute(progId, progNombre))
                        },
                    )
                }

                composable(
                    route = Screen.Programa.route,
                    arguments = listOf(
                        navArgument("programaId") { type = NavType.IntType },
                        navArgument("programaNombre") { type = NavType.StringType }
                    )
                ) { navBackStackEntry ->
                    val programaViewModel: ProgramaViewModel = viewModel(
                        key = "programa_vm_${navBackStackEntry.arguments?.getInt("programaId")}",
                        viewModelStoreOwner = navBackStackEntry,
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

                composable(
                    route = Screen.EpisodeDetail.route,
                    arguments = listOf(
                        navArgument("episodeId") { type = NavType.IntType }
                    )
                ) { navBackStackEntry ->
                    val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(
                        key = "episode_detail_vm_${navBackStackEntry.arguments?.getInt("episodeId")}",
                        viewModelStoreOwner = navBackStackEntry,
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

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

            }

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
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}