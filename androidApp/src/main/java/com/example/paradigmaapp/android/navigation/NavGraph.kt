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
import com.example.paradigmaapp.android.viewmodel.EpisodeDetailViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Define el grafo de navegación principal de la aplicación utilizando Jetpack Compose Navigation.
 * Configura el `NavHost` con todas las pantallas (rutas) y la lógica para navegar entre ellas.
 * Utiliza un [BottomSheetScaffold] para integrar un control de volumen deslizable.
 *
 * @param navController El [NavHostController] principal para la navegación. Si no se provee, se crea uno.
 * @param viewModelFactory La factoría [ViewModelFactory] utilizada para crear instancias de ViewModels
 * con sus dependencias inyectadas.
 * @param mainViewModel Instancia del [MainViewModel] global, que gestiona el estado de reproducción
 * y datos principales.
 * @param searchViewModel Instancia del [SearchViewModel] para la funcionalidad de búsqueda.
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

    // Estados observados del MainViewModel para la UI del reproductor y volumen
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()
    val volumeFromViewModel by mainViewModel.currentVolume.collectAsState()

    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()

    // ViewModels secundarios, accedidos a través de MainViewModel o instanciados aquí si es necesario
    val queueViewModel = mainViewModel.queueViewModel
    val downloadedViewModel = mainViewModel.downloadedViewModel
    val onGoingViewModel = mainViewModel.onGoingViewModel

    // SettingsViewModel con ámbito a la Activity para compartir estado con MainActivity (para el tema)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    BottomSheetScaffold(
        scaffoldState = volumeBottomSheetScaffoldState,
        sheetContent = {
            VolumeControl(
                onVolumeChanged = { newVolume -> mainViewModel.setVolume(newVolume) },
                currentVolume = volumeFromViewModel
            )
        },
        sheetPeekHeight = 0.dp, // El BottomSheet de volumen estará oculto inicialmente
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValuesFromBottomSheet ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromBottomSheet) // Aplica el padding del BottomSheetScaffold
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f) // El NavHost ocupa el espacio restante
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        mainViewModel = mainViewModel,
                        onProgramaSelected = { progId, progNombre ->
                            navController.navigate(Screen.Programa.createRoute(progId, progNombre))
                        }
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
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
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
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Queue.route) {
                    QueueScreen(
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.OnGoing.route) {
                    OnGoingEpisodioScreen(
                        onGoingEpisodioViewModel = onGoingViewModel,
                        queueViewModel = queueViewModel,
                        downloadedViewModel = downloadedViewModel,
                        onEpisodeSelected = { episodio -> mainViewModel.selectEpisode(episodio) },
                        onEpisodeLongClicked = { episodio ->
                            navController.navigate(Screen.EpisodeDetail.createRoute(episodio.id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.EpisodeDetail.route,
                    arguments = listOf(navArgument("episodeId") { type = NavType.IntType })
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

                composable(Screen.Settings.route) { // Ruta para la pantalla de Ajustes
                    // SettingsScreen utiliza la instancia de settingsViewModel con ámbito a la Activity.
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
                            try { // Envolver en try-catch por si partialExpand() falla si ya está oculto
                                sheetState.expand()
                            } catch (e: IllegalStateException) {
                                // Podría ocurrir si el sheet está en un estado inesperado.
                            }
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
                onSettingsClick = {
                    // Navegar a la pantalla de Ajustes.
                    // No se usa navigateToScreenIfDifferent aquí si queremos que siempre se pueda apilar.
                    navController.navigate(Screen.Settings.route) { //
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Función de utilidad para navegar a una pantalla solo si no es el destino actual.
 * Ayuda a evitar múltiples copias de la misma pantalla en la pila de retroceso
 * para las pestañas principales de la barra de navegación inferior.
 *
 * @param navController El [NavHostController] para realizar la navegación.
 * @param route La ruta del destino al que se desea navegar.
 */
private fun navigateToScreenIfDifferent(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route != route) {
        navController.navigate(route) {
            // PopUp hasta el inicio del grafo para evitar acumulación en la pila de retroceso.
            popUpTo(Screen.Home.route) { // O navController.graph.startDestinationRoute si es dinámico
                saveState = true // Guarda el estado de las pantallas en la pila.
            }
            launchSingleTop = true // Evita múltiples copias de la misma pantalla en la cima de la pila.
            restoreState = true // Restaura el estado al volver a navegar a esta pantalla.
        }
    }
}