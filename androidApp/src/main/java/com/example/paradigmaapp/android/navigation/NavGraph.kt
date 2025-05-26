package com.example.paradigmaapp.android.navigation

import android.net.Uri
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
    val queueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel = mainViewModel.onGoingViewModel

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
                        onProgramaSelected = { progId, progNombre -> // HomeScreen debe tener este parámetro
                            navController.navigate(Screen.Programa.createRoute(progId, progNombre))
                        },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                    )
                }

                composable(
                    route = Screen.Programa.route, // Asegúrate que Screen.Programa está definido en Screen.kt
                    arguments = listOf(
                        navArgument("programaId") { type = NavType.IntType },
                        navArgument("programaNombre") { type = NavType.StringType }
                    )
                ) { navBackStackEntry ->
                    val programaViewModel: ProgramaViewModel = viewModel(
                        viewModelStoreOwner = navBackStackEntry,
                        factory = viewModelFactory // Usa la factory global
                    )
                    val programaNombreArg = navBackStackEntry.arguments?.getString("programaNombre")?.let { Uri.decode(it) }
                        ?: programaViewModel.programaNombre

                    ProgramaScreen(
                        programaViewModel = programaViewModel,
                        mainViewModel = mainViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        programaNombreFallback = programaNombreArg,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        searchViewModel = searchViewModel, // ViewModel para la lógica de búsqueda
                        queueViewModel = queueViewModel,   // ViewModel para la lógica de cola
                        downloadedViewModel = downloadedViewModel, // ViewModel para la lógica de descargas
                        onEpisodeSelected = { episodio ->
                            mainViewModel.selectEpisode(episodio)
                            // ... (lógica de navegación opcional) ...
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadedEpisodioScreen( // Tu pantalla renombrada
                        downloadedEpisodioViewModel = downloadedViewModel,
                        queueViewModel = queueViewModel, // Pasar para EpisodioListItem
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel, // Pasar para EpisodioListItem
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.OnGoing.route) {
                    OnGoingEpisodioScreen( // Tu pantalla renombrada
                        onGoingEpisodioViewModel = onGoingViewModel,
                        queueViewModel = queueViewModel, // Pasar para EpisodioListItem
                        downloadedViewModel = downloadedViewModel, // Pasar para EpisodioListItem
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
                    Timber.d("Info click para episodio: ${episodio.title}")
                    navController.navigate("episode_detail_screen/${episodio.id}")
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

            BottomNavigationBar(
                navController = navController,
                onSearchClick = {
                    if (navController.currentDestination?.route != Screen.Search.route) {
                        navController.navigate(Screen.Search.route) { popUpTo(Screen.Home.route) }
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