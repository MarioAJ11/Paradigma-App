package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.screens.OnboardingScreen
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory

/**
 * Composable raíz y punto de entrada de la interfaz de la aplicación.
 * Actúa como un "interruptor": muestra la pantalla de bienvenida (`OnboardingScreen`)
 * o la estructura de navegación principal de la app (`NavGraph`) basándose en si
 * el usuario ya ha completado la introducción.
 *
 * @param viewModelFactory La factoría para crear instancias de ViewModels.
 * @param mainViewModel El ViewModel principal que gestiona el estado global.
 * @param searchViewModel El ViewModel para la funcionalidad de búsqueda.
 * @param settingsViewModel El ViewModel para los ajustes, que se pasará al NavGraph.
 */
@Composable
fun ParadigmaApp(
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel
) {
    val hasCompletedOnboarding by mainViewModel.onboardingCompleted.collectAsState()

    if (hasCompletedOnboarding) {
        val navController = rememberNavController()
        // Pasa la instancia del ViewModel de ajustes al grafo de navegación.
        NavGraph(
            navController = navController,
            viewModelFactory = viewModelFactory,
            mainViewModel = mainViewModel,
            searchViewModel = searchViewModel,
            settingsViewModel = settingsViewModel
        )
    } else {
        OnboardingScreen(
            onContinueClicked = {
                mainViewModel.setOnboardingComplete()
            }
        )
    }
}