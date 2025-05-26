package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.data.AppPreferences // Si alguna parte de la UI lo necesita directamente
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.viewmodel.* // Importa tus ViewModels
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory

/**
 * Composable raíz de la aplicación que configura la navegación principal
 * y la estructura de la interfaz de usuario.
 *
 * @param viewModelFactory Factory para crear ViewModels, especialmente útil para ViewModels con SavedStateHandle
 * dentro del NavGraph si se instancian allí.
 * @param mainViewModel ViewModel principal que gestiona el estado global y la reproducción.
 * @param searchViewModel ViewModel para la funcionalidad de búsqueda.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun ParadigmaApp(
    viewModelFactory: ViewModelFactory, // Para pasar a NavGraph si los ViewModels se crean por pantalla
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
    // Ya no se pasan QueueViewModel, DownloadedViewModel, OnGoingViewModel aquí directamente
    // porque MainViewModel ahora los tiene como dependencias y los coordina,
    // o NavGraph los obtendrá usando la factory con el NavBackStackEntry como owner.
) {
    val navController = rememberNavController()

    // El Theme ya se aplica en MainActivity.
    NavGraph(
        navController = navController,
        viewModelFactory = viewModelFactory, // Pasar la factory
        mainViewModel = mainViewModel,
        searchViewModel = searchViewModel
        // Los otros ViewModels principales (Queue, Downloaded, OnGoing) se acceden
        // via mainViewModel o se instancian en NavGraph con la factory.
    )
}