package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory

/**
 * Composable raíz de la aplicación Paradigma Media.
 * Configura el [NavHostController] principal y el [NavGraph] que define
 * la estructura de navegación y la presentación de las diferentes pantallas.
 *
 * @param viewModelFactory La factoría [ViewModelFactory] necesaria para la creación de
 * ViewModels con dependencias dentro del [NavGraph] (ej. ViewModels con ámbito
 * a una ruta de navegación específica que requieran [androidx.lifecycle.SavedStateHandle]).
 * @param mainViewModel Instancia del [MainViewModel] global, que gestiona el estado de
 * reproducción y datos principales.
 * @param searchViewModel Instancia del [SearchViewModel] para la funcionalidad de búsqueda.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun ParadigmaApp(
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    // Crea y recuerda el NavController para gestionar la navegación.
    val navController = rememberNavController()

    // Configura el grafo de navegación.
    NavGraph(
        navController = navController,
        viewModelFactory = viewModelFactory,
        mainViewModel = mainViewModel,
        searchViewModel = searchViewModel
    )
}