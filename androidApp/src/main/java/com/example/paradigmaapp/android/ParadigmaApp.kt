package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.viewmodel.*

/**
 * Composable raíz de la aplicación que configura la navegación principal
 * y la estructura de la interfaz de usuario.
 *
 * @param mainViewModel ViewModel principal que gestiona el estado global y la reproducción.
 * @param searchViewModel ViewModel para la funcionalidad de búsqueda.
 * @param queueViewModel ViewModel para la cola de reproducción.
 * @param downloadedViewModel ViewModel para los episodios descargados.
 * @param onGoingViewModel ViewModel para los episodios en curso.
 * @param appPreferences (Opcional aquí) Gestor de preferencias, si alguna pantalla lo necesita directamente.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun ParadigmaApp(
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodioViewModel,
    onGoingViewModel: OnGoingEpisodioViewModel,
    appPreferences: AppPreferences // Puede no ser necesario pasarlo si los ViewModels ya lo tienen
) {
    val navController = rememberNavController()

    NavGraph(
        navController = navController,
        mainViewModel = mainViewModel,
        searchViewModel = searchViewModel,
        queueViewModel = queueViewModel,
        downloadedViewModel = downloadedViewModel,
        onGoingViewModel = onGoingViewModel,
        appPreferences = appPreferences
    )
}