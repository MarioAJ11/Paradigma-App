package com.example.paradigmaapp.android.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.screens.*
import com.example.paradigmaapp.android.ui.SettingsScreen
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Define el grafo de navegación principal de la aplicación.
 * También incluye el BottomSheetScaffold y el AudioPlayer global.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onGoingViewModel: OnGoingEpisodioViewModel,
    appPreferences: AppPreferences
) {
    val coroutineScope = rememberCoroutineScope()

    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying

    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()

    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState,
        sheetContent = {
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
                    Timber.d("Dispositivo Bluetooth seleccionado: $deviceName (lógica no implementada)")
                    coroutineScope.launch { volumeBottomSheetScaffoldState.bottomSheetState.partialExpand() }
                }
            )
        },
        sheetPeekHeight = 0.dp
    ) { paddingValuesDelBottomSheet ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesDelBottomSheet)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio ->
                            mainViewModel.selectEpisode(episodio)
                        },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                    )
                }
                composable(Screen.Search.route) {
                    // Pasar todos los ViewModels necesarios para las acciones de EpisodioListItem
                    SearchScreen(
                        searchViewModel = searchViewModel,
                        queueViewModel = queueViewModel, // Pasar QueueViewModel
                        downloadedViewModel = downloadedViewModel, // Pasar DownloadedViewModel
                        onEpisodeSelected = { episodio ->
                            mainViewModel.selectEpisode(episodio)
                            navController.popBackStack()
                        },
                        onBackClick = { navController.popBackStack() }
                        // Las acciones como onAddToQueue, etc., se manejan dentro de SearchScreen
                        // usando los ViewModels que se le pasan, los cuales a su vez se los pasan a EpisodioListItem.
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen(
                        downloadedEpisodioViewModel = downloadedViewModel,
                        queueViewModel = queueViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
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
                    Timber.d("Info click para episodio: ${episodio.title} (navegación no implementada)")
                    // Ejemplo: navController.navigate("episode_detail/${episodio.id}")
                },
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

            BottomNavigationBar( // Asegúrate que este Composable existe y está importado
                onSearchClick = {
                    if (navController.currentDestination?.route != Screen.Search.route) {
                        navController.navigate(Screen.Search.route)
                    }
                },
                onOnGoingClick = { navController.navigate(Screen.OnGoing.route) { launchSingleTop = true; popUpTo(Screen.Home.route) } },
                onDownloadedClick = { navController.navigate(Screen.Downloads.route) { launchSingleTop = true; popUpTo(Screen.Home.route) } },
                onQueueClick = { navController.navigate(Screen.Queue.route) { launchSingleTop = true; popUpTo(Screen.Home.route) } },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    }

    if (showSettingsDialog) {
        SettingsScreen(
            onDismissRequest = { showSettingsDialog = false },
            isStreamActive = isAndainaStreamActive,
            onStreamActiveChanged = {
                mainViewModel.toggleAndainaStreamActive()
                showSettingsDialog = false
            }
        )
    }
}