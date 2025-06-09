package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.screens.OnboardingScreen
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory

/**
 * Composable raíz y punto de entrada de la interfaz de la aplicación.
 * Actúa como un "interruptor": muestra la pantalla de bienvenida (`OnboardingScreen`)
 * o la estructura de navegación principal de la app (`NavGraph`) basándose en si
 * el usuario ya ha completado la introducción.
 *
 * @author Mario Alguacil Juárez
 * @param viewModelFactory La factoría para crear instancias de ViewModels.
 * @param mainViewModel El ViewModel principal que gestiona el estado global.
 * @param searchViewModel El ViewModel para la funcionalidad de búsqueda.
 */
@Composable
fun ParadigmaApp(
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel
) {
    // Se suscribe al estado de 'onboarding' desde el ViewModel.
    // La UI se recompondrá automáticamente cuando este valor cambie.
    val hasCompletedOnboarding by mainViewModel.onboardingCompleted.collectAsState()

    if (hasCompletedOnboarding) {
        // --- ESTADO COMPLETADO: Muestra la aplicación principal ---
        // Crea una instancia del NavController para la navegación.
        val navController = rememberNavController()
        NavGraph(
            navController = navController,
            viewModelFactory = viewModelFactory,
            mainViewModel = mainViewModel,
            searchViewModel = searchViewModel
        )
    } else {
        // --- ESTADO INICIAL: Muestra la pantalla de bienvenida ---
        OnboardingScreen(
            onContinueClicked = {
                // Al pulsar "ACEPTAR", se actualiza el estado en el ViewModel.
                // Esto provocará que 'hasCompletedOnboarding' cambie a 'true'
                // y la UI se recomponga para mostrar el NavGraph.
                mainViewModel.setOnboardingComplete()
            }
        )
    }
}