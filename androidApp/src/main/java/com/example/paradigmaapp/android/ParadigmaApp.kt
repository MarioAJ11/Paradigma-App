package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.viewmodel.*
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
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    val navController = rememberNavController()
    NavGraph(
        navController = navController,
        viewModelFactory = viewModelFactory,
        mainViewModel = mainViewModel,
        searchViewModel = searchViewModel
    )
}